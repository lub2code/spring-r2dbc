/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.data.r2dbc.core;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.data.r2dbc.query.Update;
import org.springframework.data.r2dbc.support.R2dbcExceptionTranslator;

/**
 * A non-blocking, reactive client for performing database calls requests with Reactive Streams back pressure. Provides
 * a higher level, common API over R2DBC client libraries.
 * <p>
 * Use one of the static factory methods {@link #create(ConnectionFactory)} or obtain a {@link DatabaseClient#builder()}
 * to create an instance.
 *
 * @author Mark Paluch
 */
public interface DatabaseClient {

	/**
	 * Specify a static {@code sql} string to execute. Contract for specifying a SQL call along with options leading to
	 * the exchange. The SQL string can contain either native parameter bind markers or named parameters (e.g.
	 * {@literal :foo, :bar}) when {@link NamedParameterExpander} is enabled.
	 *
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(NamedParameterExpander)
	 * @param sql must not be {@literal null} or empty.
	 * @return a new {@link GenericExecuteSpec}.
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(NamedParameterExpander)
	 */
	GenericExecuteSpec execute(String sql);

	/**
	 * Specify a {@link Supplier SQL supplier} that provides SQL to execute. Contract for specifying a SQL call along with
	 * options leading to the exchange. The SQL string can contain either native parameter bind markers or named
	 * parameters (e.g. {@literal :foo, :bar}) when {@link NamedParameterExpander} is enabled.
	 * <p>
	 * Accepts {@link PreparedOperation} as SQL and binding {@link Supplier}.
	 * </p>
	 *
	 * @param sqlSupplier must not be {@literal null}.
	 * @return a new {@link GenericExecuteSpec}.
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(NamedParameterExpander)
	 * @see PreparedOperation
	 */
	GenericExecuteSpec execute(Supplier<String> sqlSupplier);

	/**
	 * Prepare an SQL call returning a result.
	 *
	 * @deprecated will be removed with 1.0 M3. Use {@link #execute(String)} directly.
	 */
	@Deprecated
	SqlSpec execute();

	/**
	 * Prepare an SQL SELECT call.
	 */
	SelectFromSpec select();

	/**
	 * Prepare an SQL INSERT call.
	 */
	InsertIntoSpec insert();

	/**
	 * Prepare an SQL UPDATE call.
	 */
	UpdateTableSpec update();

	/**
	 * Prepare an SQL DELETE call.
	 */
	DeleteFromSpec delete();

	/**
	 * Return a builder to mutate properties of this database client.
	 */
	DatabaseClient.Builder mutate();

	// Static, factory methods

	/**
	 * Creates a {@code DatabaseClient} that will use the provided {@link io.r2dbc.spi.ConnectionFactory}.
	 *
	 * @param factory The {@code ConnectionFactory} to use for obtaining connections.
	 * @return a new {@code DatabaseClient}. Guaranteed to be not {@literal null}.
	 */
	static DatabaseClient create(ConnectionFactory factory) {
		return new DefaultDatabaseClientBuilder().connectionFactory(factory).build();
	}

	/**
	 * Obtain a {@code DatabaseClient} builder.
	 */
	static DatabaseClient.Builder builder() {
		return new DefaultDatabaseClientBuilder();
	}

	/**
	 * A mutable builder for creating a {@link DatabaseClient}.
	 */
	interface Builder {

		/**
		 * Configures the {@link ConnectionFactory R2DBC connector}.
		 *
		 * @param factory must not be {@literal null}.
		 * @return {@code this} {@link Builder}.
		 */
		Builder connectionFactory(ConnectionFactory factory);

		/**
		 * Configures a {@link R2dbcExceptionTranslator}.
		 *
		 * @param exceptionTranslator must not be {@literal null}.
		 * @return {@code this} {@link Builder}.
		 */
		Builder exceptionTranslator(R2dbcExceptionTranslator exceptionTranslator);

		/**
		 * Configures a {@link ReactiveDataAccessStrategy}.
		 *
		 * @param accessStrategy must not be {@literal null}.
		 * @return {@code this} {@link Builder}.
		 */
		Builder dataAccessStrategy(ReactiveDataAccessStrategy accessStrategy);

		/**
		 * Configures whether to use named parameter expansion. Defaults to {@literal true}.
		 *
		 * @param enabled {@literal true} to use named parameter expansion. {@literal false} to disable named parameter
		 *          expansion.
		 * @return {@code this} {@link Builder}.
		 * @see NamedParameterExpander
		 */
		Builder namedParameters(boolean enabled);

		/**
		 * Configures a {@link Consumer} to configure this builder.
		 *
		 * @param builderConsumer must not be {@literal null}.
		 * @return {@code this} {@link Builder}.
		 */
		Builder apply(Consumer<Builder> builderConsumer);

		/**
		 * Builder the {@link DatabaseClient} instance.
		 */
		DatabaseClient build();
	}

	/**
	 * Contract for specifying a SQL call along with options leading to the exchange. The SQL string can contain either
	 * native parameter bind markers (e.g. {@literal $1, $2} for Postgres, {@literal @P0, @P1} for SQL Server) or named
	 * parameters (e.g. {@literal :foo, :bar}) when {@link NamedParameterExpander} is enabled.
	 * <p>
	 * Accepts {@link PreparedOperation} as SQL and binding {@link Supplier}.
	 * </p>
	 *
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(NamedParameterExpander)
	 * @deprecated use {@code DatabaseClient.execute(…)} directly.
	 */
	@Deprecated
	interface SqlSpec {

		/**
		 * Specify a static {@code sql} string to execute.
		 *
		 * @param sql must not be {@literal null} or empty.
		 * @return a new {@link GenericExecuteSpec}.
		 */
		@Deprecated
		GenericExecuteSpec sql(String sql);

		/**
		 * Specify a static {@link Supplier SQL supplier} that provides SQL to execute.
		 *
		 * @param sqlSupplier must not be {@literal null}.
		 * @return a new {@link GenericExecuteSpec}.
		 * @see PreparedOperation
		 */
		@Deprecated
		GenericExecuteSpec sql(Supplier<String> sqlSupplier);
	}

	/**
	 * Contract for specifying a SQL call along with options leading to the exchange.
	 */
	interface GenericExecuteSpec extends BindSpec<GenericExecuteSpec> {

		/**
		 * Define the target type the result should be mapped to. <br />
		 * Skip this step if you are anyway fine with the default conversion.
		 *
		 * @param resultType must not be {@literal null}.
		 * @param <R> result type.
		 */
		<R> TypedExecuteSpec<R> as(Class<R> resultType);

		/**
		 * Configure a result mapping {@link java.util.function.Function function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction);

		/**
		 * Configure a result mapping {@link java.util.function.BiFunction function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		FetchSpec<Map<String, Object>> fetch();

		/**
		 * Perform the SQL call and return a {@link Mono} that completes without result on statement completion.
		 *
		 * @return a {@link Mono} ignoring its payload (actively dropping).
		 */
		Mono<Void> then();
	}

	/**
	 * Contract for specifying a SQL call along with options leading to the exchange.
	 */
	interface TypedExecuteSpec<T> extends BindSpec<TypedExecuteSpec<T>> {

		/**
		 * Define the target type the result should be mapped to. <br />
		 * Skip this step if you are anyway fine with the default conversion.
		 *
		 * @param resultType must not be {@literal null}.
		 * @param <R> result type.
		 */
		<R> TypedExecuteSpec<R> as(Class<R> resultType);

		/**
		 * Configure a result mapping {@link java.util.function.Function function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction);

		/**
		 * Configure a result mapping {@link java.util.function.BiFunction function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		FetchSpec<T> fetch();

		/**
		 * Perform the SQL call and return a {@link Mono} that completes without result on statement completion.
		 *
		 * @return a {@link Mono} ignoring its payload (actively dropping).
		 */
		Mono<Void> then();
	}

	/**
	 * Contract for specifying {@code SELECT} options leading to the exchange.
	 */
	interface SelectFromSpec {

		/**
		 * Specify the source {@literal table} to select from.
		 *
		 * @param table must not be {@literal null} or empty.
		 * @return a {@link GenericSelectSpec} for further configuration of the select. Guaranteed to be not
		 *         {@literal null}.
		 */
		GenericSelectSpec from(String table);

		/**
		 * Specify the source table to select from to using the {@link Class entity class}.
		 *
		 * @param table must not be {@literal null}.
		 * @return a {@link TypedSelectSpec} for further configuration of the select. Guaranteed to be not {@literal null}.
		 */
		<T> TypedSelectSpec<T> from(Class<T> table);
	}

	/**
	 * Contract for specifying {@code INSERT} options leading to the exchange.
	 */
	interface InsertIntoSpec {

		/**
		 * Specify the target {@literal table} to insert into.
		 *
		 * @param table must not be {@literal null} or empty.
		 * @return a {@link GenericInsertSpec} for further configuration of the insert. Guaranteed to be not
		 *         {@literal null}.
		 */
		GenericInsertSpec<Map<String, Object>> into(String table);

		/**
		 * Specify the target table to insert to using the {@link Class entity class}.
		 *
		 * @param table must not be {@literal null}.
		 * @return a {@link TypedInsertSpec} for further configuration of the insert. Guaranteed to be not {@literal null}.
		 */
		<T> TypedInsertSpec<T> into(Class<T> table);
	}

	/**
	 * Contract for specifying {@code UPDATE} options leading to the exchange.
	 */
	interface UpdateTableSpec {

		/**
		 * Specify the target {@literal table} to update.
		 *
		 * @param table must not be {@literal null} or empty.
		 * @return a {@link GenericUpdateSpec} for further configuration of the update. Guaranteed to be not
		 *         {@literal null}.
		 */
		GenericUpdateSpec table(String table);

		/**
		 * Specify the target table to update to using the {@link Class entity class}.
		 *
		 * @param table must not be {@literal null}.
		 * @return a {@link TypedUpdateSpec} for further configuration of the update. Guaranteed to be not {@literal null}.
		 */
		<T> TypedUpdateSpec<T> table(Class<T> table);
	}

	/**
	 * Contract for specifying {@code DELETE} options leading to the exchange.
	 */
	interface DeleteFromSpec {

		/**
		 * Specify the source {@literal table} to delete from.
		 *
		 * @param table must not be {@literal null} or empty.
		 * @return a {@link DeleteMatchingSpec} for further configuration of the delete. Guaranteed to be not
		 *         {@literal null}.
		 */
		DeleteMatchingSpec from(String table);

		/**
		 * Specify the source table to delete from to using the {@link Class entity class}.
		 *
		 * @param table must not be {@literal null}.
		 * @return a {@link TypedDeleteSpec} for further configuration of the delete. Guaranteed to be not {@literal null}.
		 */
		<T> TypedDeleteSpec<T> from(Class<T> table);
	}

	/**
	 * Contract for specifying {@code SELECT} options leading to the exchange.
	 */
	interface GenericSelectSpec extends SelectSpec<GenericSelectSpec> {

		/**
		 * Define the target type the result should be mapped to. <br />
		 * Skip this step if you are anyway fine with the default conversion.
		 *
		 * @param resultType must not be {@literal null}.
		 * @param <R> result type.
		 */
		<R> TypedSelectSpec<R> as(Class<R> resultType);

		/**
		 * Configure a result mapping {@link java.util.function.Function function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction);

		/**
		 * Configure a result mapping {@link java.util.function.BiFunction function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		FetchSpec<Map<String, Object>> fetch();
	}

	/**
	 * Contract for specifying {@code SELECT} options leading to the exchange.
	 */
	interface TypedSelectSpec<T> extends SelectSpec<TypedSelectSpec<T>> {

		/**
		 * Define the target type the result should be mapped to. <br />
		 * Skip this step if you are anyway fine with the default conversion.
		 *
		 * @param resultType must not be {@literal null}.
		 * @param <R> result type.
		 */
		<R> RowsFetchSpec<R> as(Class<R> resultType);

		/**
		 * Configure a result mapping {@link java.util.function.Function function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction);

		/**
		 * Configure a result mapping {@link java.util.function.BiFunction function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		FetchSpec<T> fetch();
	}

	/**
	 * Contract for specifying {@code SELECT} options leading to the exchange.
	 */
	interface SelectSpec<S extends SelectSpec<S>> {

		/**
		 * Configure projected fields.
		 *
		 * @param selectedFields must not be {@literal null}.
		 */
		S project(String... selectedFields);

		/**
		 * Configure a filter {@link Criteria}.
		 *
		 * @param criteria must not be {@literal null}.
		 */
		S matching(Criteria criteria);

		/**
		 * Configure {@link Sort}.
		 *
		 * @param sort must not be {@literal null}.
		 */
		S orderBy(Sort sort);

		/**
		 * Configure {@link Sort}.
		 *
		 * @param orders must not be {@literal null}.
		 */
		default S orderBy(Sort.Order... orders) {
			return orderBy(Sort.by(orders));
		}

		/**
		 * Configure pagination. Overrides {@link Sort} if the {@link Pageable} contains a {@link Sort} object.
		 *
		 * @param pageable must not be {@literal null}.
		 */
		S page(Pageable pageable);
	}

	/**
	 * Contract for specifying {@code INSERT} options leading to the exchange.
	 *
	 * @param <T> Result type of tabular insert results.
	 */
	interface GenericInsertSpec<T> extends InsertSpec<T> {

		/**
		 * Specify a field and non-{@literal null} value to insert.
		 *
		 * @param field must not be {@literal null} or empty.
		 * @param value must not be {@literal null}
		 */
		GenericInsertSpec<T> value(String field, Object value);

		/**
		 * Specify a {@literal null} value to insert.
		 *
		 * @param field must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 * @deprecated will be removed soon. Use {@link #nullValue(String)}.
		 */
		@Deprecated
		default GenericInsertSpec<T> nullValue(String field, Class<?> type) {
			return value(field, SettableValue.empty(type));
		}

		/**
		 * Specify a {@literal null} value to insert.
		 *
		 * @param field must not be {@literal null} or empty.
		 */
		GenericInsertSpec<T> nullValue(String field);
	}

	/**
	 * Contract for specifying {@code INSERT} options leading the exchange.
	 */
	interface TypedInsertSpec<T> {

		/**
		 * Insert the given {@code objectToInsert}.
		 *
		 * @param objectToInsert the object of which the attributes will provide the values for the insert. Must not be
		 *          {@literal null}.
		 * @return a {@link InsertSpec} for further configuration of the insert. Guaranteed to be not {@literal null}.
		 */
		InsertSpec<Map<String, Object>> using(T objectToInsert);

		/**
		 * Use the given {@code tableName} as insert target.
		 *
		 * @param tableName must not be {@literal null} or empty.
		 * @return a {@link TypedInsertSpec} for further configuration of the insert. Guaranteed to be not {@literal null}.
		 */
		TypedInsertSpec<T> table(String tableName);

		/**
		 * Insert the given {@link Publisher} to insert one or more objects. Inserts only a single object when calling
		 * {@link FetchSpec#one()} or {@link FetchSpec#first()}.
		 *
		 * @param objectToInsert a publisher providing the objects of which the attributes will provide the values for the
		 *          insert. Must not be {@literal null}.
		 * @return a {@link InsertSpec} for further configuration of the insert. Guaranteed to be not {@literal null}.
		 * @see InsertSpec#fetch()
		 */
		InsertSpec<Map<String, Object>> using(Publisher<T> objectToInsert);
	}

	/**
	 * Contract for specifying {@code INSERT} options leading to the exchange.
	 *
	 * @param <T> Result type of tabular insert results.
	 */
	interface InsertSpec<T> {

		/**
		 * Configure a result mapping {@link java.util.function.Function function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction);

		/**
		 * Configure a result mapping {@link java.util.function.BiFunction function}.
		 *
		 * @param mappingFunction must not be {@literal null}.
		 * @param <R> result type.
		 * @return a {@link FetchSpec} for configuration what to fetch. Guaranteed to be not {@literal null}.
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		FetchSpec<T> fetch();

		/**
		 * Perform the SQL call and return a {@link Mono} that completes without result on statement completion.
		 *
		 * @return a {@link Mono} ignoring its payload (actively dropping).
		 */
		Mono<Void> then();
	}

	/**
	 * Contract for specifying {@code UPDATE} options leading to the exchange.
	 */
	interface GenericUpdateSpec {

		/**
		 * Specify an {@link Update} object containing assignments.
		 *
		 * @param update must not be {@literal null}.
		 */
		UpdateMatchingSpec using(Update update);
	}

	/**
	 * Contract for specifying {@code UPDATE} options leading to the exchange.
	 */
	interface TypedUpdateSpec<T> {

		/**
		 * Update the given {@code objectToUpdate}.
		 *
		 * @param objectToUpdate the object of which the attributes will provide the values for the update and the primary
		 *          key. Must not be {@literal null}.
		 * @return a {@link UpdateSpec} for further configuration of the update. Guaranteed to be not {@literal null}.
		 */
		UpdateSpec using(T objectToUpdate);

		/**
		 * Use the given {@code tableName} as update target.
		 *
		 * @param tableName must not be {@literal null} or empty.
		 * @return a {@link TypedUpdateSpec} for further configuration of the update. Guaranteed to be not {@literal null}.
		 */
		TypedUpdateSpec<T> table(String tableName);
	}

	/**
	 * Contract for specifying {@code UPDATE} options leading to the exchange.
	 */
	interface UpdateMatchingSpec extends UpdateSpec {

		/**
		 * Configure a filter {@link Criteria}.
		 *
		 * @param criteria must not be {@literal null}.
		 */
		UpdateSpec matching(Criteria criteria);
	}

	/**
	 * Contract for specifying {@code UPDATE} options leading to the exchange.
	 */
	interface UpdateSpec {

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		UpdatedRowsFetchSpec fetch();

		/**
		 * Perform the SQL call and return a {@link Mono} that completes without result on statement completion.
		 *
		 * @return a {@link Mono} ignoring its payload (actively dropping).
		 */
		Mono<Void> then();
	}

	/**
	 * Contract for specifying {@code DELETE} options leading to the exchange.
	 */
	interface TypedDeleteSpec<T> extends DeleteSpec {

		/**
		 * Use the given {@code tableName} as delete target.
		 *
		 * @param tableName must not be {@literal null} or empty.
		 * @return a {@link TypedDeleteSpec} for further configuration of the delete. Guaranteed to be not {@literal null}.
		 */
		TypedDeleteSpec<T> table(String tableName);

		/**
		 * Configure a filter {@link Criteria}.
		 *
		 * @param criteria must not be {@literal null}.
		 */
		DeleteSpec matching(Criteria criteria);
	}

	/**
	 * Contract for specifying {@code DELETE} options leading to the exchange.
	 */
	interface DeleteMatchingSpec extends DeleteSpec {

		/**
		 * Configure a filter {@link Criteria}.
		 *
		 * @param criteria must not be {@literal null}.
		 */
		DeleteSpec matching(Criteria criteria);
	}

	/**
	 * Contract for specifying {@code DELETE} options leading to the exchange.
	 */
	interface DeleteSpec {

		/**
		 * Perform the SQL call and retrieve the result.
		 */
		UpdatedRowsFetchSpec fetch();

		/**
		 * Perform the SQL call and return a {@link Mono} that completes without result on statement completion.
		 *
		 * @return a {@link Mono} ignoring its payload (actively dropping).
		 */
		Mono<Void> then();
	}

	/**
	 * Contract for specifying parameter bindings.
	 */
	interface BindSpec<S extends BindSpec<S>> {

		/**
		 * Bind a non-{@literal null} value to a parameter identified by its {@code index}.
		 *
		 * @param index zero based index to bind the parameter to.
		 * @param value to bind. Must not be {@literal null}.
		 */
		S bind(int index, Object value);

		/**
		 * Bind a {@literal null} value to a parameter identified by its {@code index}.
		 *
		 * @param index zero based index to bind the parameter to.
		 * @param type must not be {@literal null}.
		 */
		S bindNull(int index, Class<?> type);

		/**
		 * Bind a non-{@literal null} value to a parameter identified by its {@code name}.
		 *
		 * @param name must not be {@literal null} or empty.
		 * @param value must not be {@literal null}.
		 */
		S bind(String name, Object value);

		/**
		 * Bind a {@literal null} value to a parameter identified by its {@code name}.
		 *
		 * @param name must not be {@literal null} or empty.
		 * @param type must not be {@literal null}.
		 */
		S bindNull(String name, Class<?> type);

		/**
		 * Bind a bean according to Java {@link java.beans.BeanInfo Beans} using property names.
		 *
		 * @param bean must not be {@literal null}.
		 */
		S bind(Object bean);
	}
}
