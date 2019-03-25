/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.r2dbc.domain;

import io.r2dbc.spi.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.springframework.data.r2dbc.dialect.BindMarker;
import org.springframework.data.r2dbc.dialect.BindMarkers;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object representing value and {@code NULL} bindings for a {@link Statement} using {@link BindMarkers}.
 *
 * @author Mark Paluch
 */
public class Bindings implements Streamable<Bindings.Binding> {

	private final Map<BindMarker, Binding> bindings;

	/**
	 * Create empty {@link Bindings}.
	 */
	public Bindings() {
		this.bindings = Collections.emptyMap();
	}

	/**
	 * Create {@link Bindings} from a {@link Map}.
	 *
	 * @param bindings must not be {@literal null}.
	 */
	public Bindings(Collection<Binding> bindings) {

		Assert.notNull(bindings, "Bindings must not be null");

		Map<BindMarker, Binding> mapping = new LinkedHashMap<>(bindings.size());
		bindings.forEach(it -> mapping.put(it.getBindMarker(), it));
		this.bindings = mapping;
	}

	Bindings(Map<BindMarker, Binding> bindings) {
		this.bindings = bindings;
	}

	protected Map<BindMarker, Binding> getBindings() {
		return bindings;
	}

	/**
	 * Merge this bindings with an other {@link Bindings} object and create a new merged {@link Bindings} object.
	 *
	 * @param left the left object to merge with.
	 * @param right the right object to merge with.
	 * @return a new, merged {@link Bindings} object.
	 */
	public static Bindings merge(Bindings left, Bindings right) {

		Assert.notNull(left, "Left side Bindings must not be null");
		Assert.notNull(right, "Right side Bindings must not be null");

		List<Binding> result = new ArrayList<>(left.getBindings().size() + right.getBindings().size());

		result.addAll(left.getBindings().values());
		result.addAll(right.getBindings().values());

		return new Bindings(result);
	}

	/**
	 * Apply the bindings to a {@link Statement}.
	 *
	 * @param statement the statement to apply to.
	 */
	public void apply(Statement statement) {

		Assert.notNull(statement, "Statement must not be null");
		this.bindings.forEach((marker, binding) -> binding.apply(statement));
	}

	/**
	 * Performs the given action for each binding of this {@link Bindings} until all bindings have been processed or the
	 * action throws an exception. Actions are performed in the order of iteration (if an iteration order is specified).
	 * Exceptions thrown by the action are relayed to the
	 *
	 * @param action The action to be performed for each {@link Binding}.
	 */
	public void forEach(Consumer<? super Binding> action) {
		this.bindings.forEach((marker, binding) -> action.accept(binding));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Binding> iterator() {
		return this.bindings.values().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#spliterator()
	 */
	@Override
	public Spliterator<Binding> spliterator() {
		return this.bindings.values().spliterator();
	}

	/**
	 * Base class for value objects representing a value or a {@code NULL} binding.
	 */
	public abstract static class Binding {

		private final BindMarker marker;

		protected Binding(BindMarker marker) {
			this.marker = marker;
		}

		/**
		 * @return the associated {@link BindMarker}.
		 */
		public BindMarker getBindMarker() {
			return marker;
		}

		/**
		 * Return {@literal true} if there is a value present, otherwise {@literal false} for a {@code NULL} binding.
		 *
		 * @return {@literal true} if there is a value present, otherwise {@literal false} for a {@code NULL} binding.
		 */
		public abstract boolean hasValue();

		/**
		 * Return {@literal true} if this is is a {@code NULL} binding.
		 *
		 * @return {@literal true} if this is is a {@code NULL} binding.
		 */
		public boolean isNull() {
			return !hasValue();
		}

		/**
		 * Returns the value of this binding. Can be {@literal null} if this is a {@code NULL} binding.
		 *
		 * @return value of this binding. Can be {@literal null} if this is a {@code NULL} binding.
		 */
		@Nullable
		public abstract Object getValue();

		/**
		 * Applies the binding to a {@link Statement}.
		 *
		 * @param statement the statement to apply to.
		 */
		public abstract void apply(Statement statement);
	}

	/**
	 * Value binding.
	 */
	public static class ValueBinding extends Binding {

		private final Object value;

		public ValueBinding(BindMarker marker, Object value) {
			super(marker);
			this.value = value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#hasValue()
		 */
		public boolean hasValue() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#getValue()
		 */
		public Object getValue() {
			return value;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#apply(io.r2dbc.spi.Statement)
		 */
		@Override
		public void apply(Statement statement) {
			getBindMarker().bind(statement, getValue());
		}
	}

	/**
	 * {@code NULL} binding.
	 */
	public static class NullBinding extends Binding {

		private final Class<?> valueType;

		public NullBinding(BindMarker marker, Class<?> valueType) {
			super(marker);
			this.valueType = valueType;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#hasValue()
		 */
		public boolean hasValue() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#getValue()
		 */
		@Nullable
		public Object getValue() {
			return null;
		}

		public Class<?> getValueType() {
			return valueType;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.r2dbc.function.query.Bindings.Binding#apply(io.r2dbc.spi.Statement)
		 */
		@Override
		public void apply(Statement statement) {
			getBindMarker().bindNull(statement, getValueType());
		}
	}
}
