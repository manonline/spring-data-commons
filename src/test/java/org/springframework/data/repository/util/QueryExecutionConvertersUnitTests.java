/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.util;

import static org.assertj.core.api.Assertions.*;

import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.Traversable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;
import scala.Option;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link QueryExecutionConverters}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class QueryExecutionConvertersUnitTests {

	DefaultConversionService conversionService;

	@Before
	public void setUp() {

		this.conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void registersWrapperTypes() {

		assertThat(QueryExecutionConverters.supports(Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(java.util.Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Future.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(ListenableFuture.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Option.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(javaslang.control.Option.class), is(true));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void registersReactiveWrapperTypes() {

		assertThat(QueryExecutionConverters.supports(Publisher.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Mono.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Flux.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Single.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Completable.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(Observable.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.reactivex.Single.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.reactivex.Maybe.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.reactivex.Completable.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.reactivex.Flowable.class)).isTrue();
		assertThat(QueryExecutionConverters.supports(io.reactivex.Observable.class)).isTrue();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void registersUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(java.util.Optional.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Future.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(ListenableFuture.class)).isTrue();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Option.class)).isTrue();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void doesNotRegisterReactiveUnwrapperTypes() {

		assertThat(QueryExecutionConverters.supportsUnwrapping(Publisher.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Mono.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Flux.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Single.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Completable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(Observable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Single.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Maybe.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Completable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Flowable.class)).isFalse();
		assertThat(QueryExecutionConverters.supportsUnwrapping(io.reactivex.Observable.class)).isFalse();
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void registersCompletableFutureAsWrapperTypeOnSpring42OrBetter() {
		assertThat(QueryExecutionConverters.supports(CompletableFuture.class)).isTrue();
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void turnsNullIntoGuavaOptional() {
		assertThat(conversionService.convert(new NullableWrapper(null), Optional.class)).isEqualTo(Optional.absent());
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoJdk8Optional() {
		assertThat(conversionService.convert(new NullableWrapper(null), java.util.Optional.class)).isEmpty();
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void turnsNullIntoCompletableFutureForNull() throws Exception {

		CompletableFuture<Object> result = conversionService.convert(new NullableWrapper(null), CompletableFuture.class);

		assertThat(result).isNotNull();
		assertThat(result.isDone()).isTrue();
		assertThat(result.get()).isNull();
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsJdk8Optional() {
		assertThat(QueryExecutionConverters.unwrap(java.util.Optional.of("Foo"))).isEqualTo("Foo");
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsGuava8Optional() {
		assertThat(QueryExecutionConverters.unwrap(Optional.of("Foo"))).isEqualTo("Foo");
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsNullToNull() {
		assertThat(QueryExecutionConverters.unwrap(null)).isNull();
	}

	/**
	 * @see DATACMNS-768
	 */
	@Test
	public void unwrapsNonWrapperTypeToItself() {
		assertThat(QueryExecutionConverters.unwrap("Foo")).isEqualTo("Foo");
	}

	/**
	 * @see DATACMNS-795
	 */
	@Test
	public void turnsNullIntoScalaOptionEmpty() {
		assertThat(conversionService.convert(new NullableWrapper(null), Option.class)).isEqualTo(Option.<Object>empty());
	}

	/**
	 * @see DATACMNS-795
	 */
	@Test
	public void unwrapsScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.apply("foo"))).isEqualTo("foo");
	}

	/**
	 * @see DATACMNS-874
	 */
	@Test
	public void unwrapsEmptyScalaOption() {
		assertThat(QueryExecutionConverters.unwrap(Option.empty())).isNull();
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void turnsNullIntoJavaslangOption() {
		assertThat(conversionService.convert(new NullableWrapper(null), javaslang.control.Option.class),
				is((Object) optionNone()));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void wrapsValueIntoJavaslangOption() {

		javaslang.control.Option<?> result = conversionService.convert(new NullableWrapper("string"),
				javaslang.control.Option.class);

		assertThat(result.isEmpty(), is(false));
		assertThat(result.get(), is((Object) "string"));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void unwrapsEmptyJavaslangOption() {
		assertThat(QueryExecutionConverters.unwrap(optionNone()), is(nullValue()));
	}

	/**
	 * @see DATACMNS-937
	 */
	@Test
	public void unwrapsJavaslangOption() {
		assertThat(QueryExecutionConverters.unwrap(option("string")), is((Object) "string"));
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	public void conversListToJavaslang() {

		assertThat(conversionService.canConvert(List.class, javaslang.collection.Traversable.class), is(true));
		assertThat(conversionService.canConvert(List.class, javaslang.collection.List.class), is(true));
		assertThat(conversionService.canConvert(List.class, javaslang.collection.Set.class), is(true));
		assertThat(conversionService.canConvert(List.class, javaslang.collection.Map.class), is(false));

		List<Integer> integers = Arrays.asList(1, 2, 3);

		Traversable<?> result = conversionService.convert(integers, Traversable.class);

		assertThat(result, is(instanceOf(javaslang.collection.List.class)));
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	public void convertsSetToJavaslang() {

		assertThat(conversionService.canConvert(Set.class, javaslang.collection.Traversable.class), is(true));
		assertThat(conversionService.canConvert(Set.class, javaslang.collection.Set.class), is(true));
		assertThat(conversionService.canConvert(Set.class, javaslang.collection.List.class), is(true));
		assertThat(conversionService.canConvert(Set.class, javaslang.collection.Map.class), is(false));

		Set<Integer> integers = Collections.singleton(1);

		Traversable<?> result = conversionService.convert(integers, Traversable.class);

		assertThat(result, is(instanceOf(javaslang.collection.Set.class)));
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	public void convertsMapToJavaslang() {

		assertThat(conversionService.canConvert(Map.class, javaslang.collection.Traversable.class), is(true));
		assertThat(conversionService.canConvert(Map.class, javaslang.collection.Map.class), is(true));
		assertThat(conversionService.canConvert(Map.class, javaslang.collection.Set.class), is(false));
		assertThat(conversionService.canConvert(Map.class, javaslang.collection.List.class), is(false));

		Map<String, String> map = Collections.singletonMap("key", "value");

		Traversable<?> result = conversionService.convert(map, Traversable.class);

		assertThat(result, is(instanceOf(javaslang.collection.Map.class)));
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	public void unwrapsJavaslangCollectionsToJavaOnes() {

		assertThat(unwrap(javaslangList(1, 2, 3)), is(instanceOf(List.class)));
		assertThat(unwrap(javaslangSet(1, 2, 3)), is(instanceOf(Set.class)));
		assertThat(unwrap(javaslangMap("key", "value")), is(instanceOf(Map.class)));
	}

	@SuppressWarnings("unchecked")
	private static javaslang.control.Option<Object> optionNone() {

		Method method = ReflectionUtils.findMethod(javaslang.control.Option.class, "none");
		return (javaslang.control.Option<Object>) ReflectionUtils.invokeMethod(method, null);
	}

	@SuppressWarnings("unchecked")
	private static <T> javaslang.control.Option<T> option(T source) {

		Method method = ReflectionUtils.findMethod(javaslang.control.Option.class, "of", Object.class);
		return (javaslang.control.Option<T>) ReflectionUtils.invokeMethod(method, null, source);
	}

	@SuppressWarnings("unchecked")
	private static <T> javaslang.collection.List<T> javaslangList(T... values) {

		Method method = ReflectionUtils.findMethod(javaslang.collection.List.class, "ofAll", Iterable.class);
		return (javaslang.collection.List<T>) ReflectionUtils.invokeMethod(method, null, Arrays.asList(values));
	}

	@SuppressWarnings("unchecked")
	private static <T> javaslang.collection.Set<T> javaslangSet(T... values) {

		Method method = ReflectionUtils.findMethod(HashSet.class, "ofAll", Iterable.class);
		return (javaslang.collection.Set<T>) ReflectionUtils.invokeMethod(method, null, Arrays.asList(values));
	}

	@SuppressWarnings("unchecked")
	private static <K, V> javaslang.collection.Map<K, V> javaslangMap(K key, V value) {

		Method method = ReflectionUtils.findMethod(HashMap.class, "ofAll", Map.class);
		return (javaslang.collection.Map<K, V>) ReflectionUtils.invokeMethod(method, null,
				Collections.singletonMap(key, value));
	}
}
