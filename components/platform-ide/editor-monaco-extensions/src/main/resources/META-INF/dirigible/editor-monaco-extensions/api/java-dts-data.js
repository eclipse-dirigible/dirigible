/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
export const dts = `

/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.nio.file.attribute.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.util.zip' {
    import { Cloneable, String, InternalError, Exception } from '@java/java.lang'
    import { LocalDateTime } from '@java/java.time'
    import { Enumeration } from '@java/java.util'
    import { FilterInputStream, InputStream, OutputStream, Closeable, IOException, FilterOutputStream, File } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { ByteBuffer } from '@java/java.nio'
    import { FileTime } from '@java/java.nio.file.attribute'
    import { Charset } from '@java/java.nio.charset'
    export interface Adler32 extends Checksum { }
    export class Adler32 implements Checksum {
        constructor();

        update(arg0: number): void;

        update(arg0: number[], arg1: number, arg2: number): void;

        update(arg0: ByteBuffer): void;

        reset(): void;

        getValue(): number;
    }

    export interface CRC32 extends Checksum { }
    export class CRC32 implements Checksum {
        constructor();

        update(arg0: number): void;

        update(arg0: number[], arg1: number, arg2: number): void;

        update(arg0: ByteBuffer): void;

        reset(): void;

        getValue(): number;
    }

    export interface CRC32C extends Checksum { }
    export class CRC32C implements Checksum {
        constructor();

        update(arg0: number): void;

        update(arg0: number[], arg1: number, arg2: number): void;

        update(arg0: ByteBuffer): void;

        reset(): void;

        getValue(): number;
    }

    export class CheckedInputStream extends FilterInputStream {
        constructor(arg0: InputStream, arg1: Checksum);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        getChecksum(): Checksum;
    }

    export class CheckedOutputStream extends FilterOutputStream {
        constructor(arg0: OutputStream, arg1: Checksum);

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        getChecksum(): Checksum;
    }

    export interface Checksum {

        update(arg0: number): void;

/* default */ update(arg0: number[]): void;

        update(arg0: number[], arg1: number, arg2: number): void;

/* default */ update(arg0: ByteBuffer): void;

        getValue(): number;

        reset(): void;
    }

    export class DataFormatException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export class Deflater {
        static DEFLATED: number
        static NO_COMPRESSION: number
        static BEST_SPEED: number
        static BEST_COMPRESSION: number
        static DEFAULT_COMPRESSION: number
        static FILTERED: number
        static HUFFMAN_ONLY: number
        static DEFAULT_STRATEGY: number
        static NO_FLUSH: number
        static SYNC_FLUSH: number
        static FULL_FLUSH: number
        constructor(arg0: number, arg1: boolean);
        constructor(arg0: number);
        constructor();

        setInput(arg0: number[], arg1: number, arg2: number): void;

        setInput(arg0: number[]): void;

        setInput(arg0: ByteBuffer): void;

        setDictionary(arg0: number[], arg1: number, arg2: number): void;

        setDictionary(arg0: number[]): void;

        setDictionary(arg0: ByteBuffer): void;

        setStrategy(arg0: number): void;

        setLevel(arg0: number): void;

        needsInput(): boolean;

        finish(): void;

        finished(): boolean;

        deflate(arg0: number[], arg1: number, arg2: number): number;

        deflate(arg0: number[]): number;

        deflate(arg0: ByteBuffer): number;

        deflate(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        deflate(arg0: ByteBuffer, arg1: number): number;

        getAdler(): number;

        getTotalIn(): number;

        getBytesRead(): number;

        getTotalOut(): number;

        getBytesWritten(): number;

        reset(): void;

        end(): void;
    }

    export class DeflaterInputStream extends FilterInputStream {
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: Deflater);
        constructor(arg0: InputStream, arg1: Deflater, arg2: number);

        close(): void;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        available(): number;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;
    }

    export class DeflaterOutputStream extends FilterOutputStream {
        constructor(arg0: OutputStream, arg1: Deflater, arg2: number, arg3: boolean);
        constructor(arg0: OutputStream, arg1: Deflater, arg2: number);
        constructor(arg0: OutputStream, arg1: Deflater, arg2: boolean);
        constructor(arg0: OutputStream, arg1: Deflater);
        constructor(arg0: OutputStream, arg1: boolean);
        constructor(arg0: OutputStream);

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        finish(): void;

        close(): void;

        flush(): void;
    }

    export class GZIPInputStream extends InflaterInputStream {
        static GZIP_MAGIC: number
        constructor(arg0: InputStream, arg1: number);
        constructor(arg0: InputStream);

        read(arg0: number[], arg1: number, arg2: number): number;

        close(): void;
    }

    export class GZIPOutputStream extends DeflaterOutputStream {
        constructor(arg0: OutputStream, arg1: number);
        constructor(arg0: OutputStream, arg1: number, arg2: boolean);
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: boolean);

        write(arg0: number[], arg1: number, arg2: number): void;

        finish(): void;
    }

    export class Inflater {
        constructor(arg0: boolean);
        constructor();

        setInput(arg0: number[], arg1: number, arg2: number): void;

        setInput(arg0: number[]): void;

        setInput(arg0: ByteBuffer): void;

        setDictionary(arg0: number[], arg1: number, arg2: number): void;

        setDictionary(arg0: number[]): void;

        setDictionary(arg0: ByteBuffer): void;

        getRemaining(): number;

        needsInput(): boolean;

        needsDictionary(): boolean;

        finished(): boolean;

        inflate(arg0: number[], arg1: number, arg2: number): number;

        inflate(arg0: number[]): number;

        inflate(arg0: ByteBuffer): number;

        getAdler(): number;

        getTotalIn(): number;

        getBytesRead(): number;

        getTotalOut(): number;

        getBytesWritten(): number;

        reset(): void;

        end(): void;
    }

    export class InflaterInputStream extends FilterInputStream {
        constructor(arg0: InputStream, arg1: Inflater, arg2: number);
        constructor(arg0: InputStream, arg1: Inflater);
        constructor(arg0: InputStream);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        available(): number;

        skip(arg0: number): number;

        close(): void;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;
    }

    export class InflaterOutputStream extends FilterOutputStream {
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: Inflater);
        constructor(arg0: OutputStream, arg1: Inflater, arg2: number);

        close(): void;

        flush(): void;

        finish(): void;

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;
    }

    export class ZipEntry implements ZipConstants, Cloneable {
        static STORED: number
        static DEFLATED: number
        constructor(arg0: String);
        constructor(arg0: ZipEntry);

        getName(): String;

        setTime(arg0: number): void;

        getTime(): number;

        setTimeLocal(arg0: LocalDateTime): void;

        getTimeLocal(): LocalDateTime;

        setLastModifiedTime(arg0: FileTime): ZipEntry;

        getLastModifiedTime(): FileTime;

        setLastAccessTime(arg0: FileTime): ZipEntry;

        getLastAccessTime(): FileTime;

        setCreationTime(arg0: FileTime): ZipEntry;

        getCreationTime(): FileTime;

        setSize(arg0: number): void;

        getSize(): number;

        getCompressedSize(): number;

        setCompressedSize(arg0: number): void;

        setCrc(arg0: number): void;

        getCrc(): number;

        setMethod(arg0: number): void;

        getMethod(): number;

        setExtra(arg0: number[]): void;

        getExtra(): number[];

        setComment(arg0: String): void;

        getComment(): String;

        isDirectory(): boolean;
        toString(): string;

        hashCode(): number;

        clone(): Object;
    }

    export class ZipError extends InternalError {
        constructor(arg0: String);
    }

    export class ZipException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export class ZipFile implements ZipConstants, Closeable {
        static OPEN_READ: number
        static OPEN_DELETE: number
        constructor(arg0: String);
        constructor(arg0: File, arg1: number);
        constructor(arg0: File);
        constructor(arg0: File, arg1: number, arg2: Charset);
        constructor(arg0: String, arg1: Charset);
        constructor(arg0: File, arg1: Charset);

        getComment(): String;

        getEntry(arg0: String): ZipEntry;

        getInputStream(arg0: ZipEntry): InputStream;

        getName(): String;

        entries(): Enumeration<ZipEntry>;

        stream(): Stream<ZipEntry>;

        size(): number;

        close(): void;
    }

    export class ZipInputStream extends InflaterInputStream implements ZipConstants {
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: Charset);

        getNextEntry(): ZipEntry;

        closeEntry(): void;

        available(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        close(): void;
    }

    export class ZipOutputStream extends DeflaterOutputStream implements ZipConstants {
        static STORED: number
        static DEFLATED: number
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: Charset);

        setComment(arg0: String): void;

        setMethod(arg0: number): void;

        setLevel(arg0: number): void;

        putNextEntry(arg0: ZipEntry): void;

        closeEntry(): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        finish(): void;

        close(): void;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.util.stream' {
    import { Enum, Integer, AutoCloseable, Runnable, CharSequence, Long, Class, String, Boolean, Double } from '@java/java.lang'
    import { Set, Optional, PrimitiveIterator, OptionalInt, DoubleSummaryStatistics, IntSummaryStatistics, OptionalDouble, Comparator, LongSummaryStatistics, Iterator, Collection, OptionalLong, List, Map, Spliterator } from '@java/java.util'
    import { ConcurrentMap } from '@java/java.util.concurrent'
    import { IntSupplier, DoubleToLongFunction, IntUnaryOperator, IntToDoubleFunction, Predicate, ObjDoubleConsumer, Function, ToLongFunction, Consumer, LongToIntFunction, ObjLongConsumer, Supplier, BinaryOperator, ObjIntConsumer, UnaryOperator, ToDoubleFunction, DoubleUnaryOperator, LongBinaryOperator, DoublePredicate, IntPredicate, LongPredicate, IntConsumer, LongToDoubleFunction, DoubleFunction, LongConsumer, DoubleConsumer, DoubleBinaryOperator, LongFunction, BiFunction, DoubleSupplier, IntFunction, IntBinaryOperator, LongUnaryOperator, IntToLongFunction, ToIntFunction, LongSupplier, BiConsumer, DoubleToIntFunction } from '@java/java.util.function'
    export interface BaseStream<T extends Object, S extends BaseStream<T, S>> extends AutoCloseable, Object {

        iterator(): Iterator<T>;

        spliterator(): Spliterator<T>;

        isParallel(): boolean;

        sequential(): S;

        parallel(): S;

        unordered(): S;

        onClose(arg0: Runnable): S;

        close(): void;
    }

    export namespace Collector {
        function
/* default */ of<T extends Object, R extends Object>(arg0: Supplier<R>, arg1: BiConsumer<R, T>, arg2: BinaryOperator<R>, arg3: Collector.Characteristics[]): Collector<T, R, R>;
        function
/* default */ of<T extends Object, A extends Object, R extends Object>(arg0: Supplier<A>, arg1: BiConsumer<A, T>, arg2: BinaryOperator<A>, arg3: Function<A, R>, arg4: Collector.Characteristics[]): Collector<T, A, R>;
    }

    export interface Collector<T extends Object, A extends Object, R extends Object> extends Object {

        supplier(): Supplier<A>;

        accumulator(): BiConsumer<A, T>;

        combiner(): BinaryOperator<A>;

        finisher(): Function<A, R>;

        characteristics(): Set<Collector.Characteristics>;
    }
    export namespace Collector {
        export class Characteristics extends Enum<Collector.Characteristics> {
            static CONCURRENT: Collector.Characteristics
            static UNORDERED: Collector.Characteristics
            static IDENTITY_FINISH: Collector.Characteristics

            static values(): Collector.Characteristics[];

            static valueOf(arg0: String): Collector.Characteristics;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class Collectors {

        static toCollection<T extends Object, C extends Collection<T>>(arg0: Supplier<C>): Collector<T, any, C>;

        static toList<T extends Object>(): Collector<T, any, List<T>>;

        static toUnmodifiableList<T extends Object>(): Collector<T, any, List<T>>;

        static toSet<T extends Object>(): Collector<T, any, Set<T>>;

        static toUnmodifiableSet<T extends Object>(): Collector<T, any, Set<T>>;

        static joining(): Collector<CharSequence, any, String>;

        static joining(arg0: CharSequence): Collector<CharSequence, any, String>;

        static joining(arg0: CharSequence, arg1: CharSequence, arg2: CharSequence): Collector<CharSequence, any, String>;

        static mapping<T extends Object, U extends Object, A extends Object, R extends Object>(arg0: Function<T, U>, arg1: Collector<U, A, R>): Collector<T, any, R>;

        static flatMapping<T extends Object, U extends Object, A extends Object, R extends Object>(arg0: Function<T, Stream<U>>, arg1: Collector<U, A, R>): Collector<T, any, R>;

        static filtering<T extends Object, A extends Object, R extends Object>(arg0: Predicate<T>, arg1: Collector<T, A, R>): Collector<T, any, R>;

        static collectingAndThen<T extends Object, A extends Object, R extends Object, RR extends Object>(arg0: Collector<T, A, R>, arg1: Function<R, RR>): Collector<T, A, RR>;

        static counting<T extends Object>(): Collector<T, any, Number>;

        static minBy<T extends Object>(arg0: Comparator<T>): Collector<T, any, Optional<T>>;

        static maxBy<T extends Object>(arg0: Comparator<T>): Collector<T, any, Optional<T>>;

        static summingInt<T extends Object>(arg0: ToIntFunction<T>): Collector<T, any, Number>;

        static summingLong<T extends Object>(arg0: ToLongFunction<T>): Collector<T, any, Number>;

        static summingDouble<T extends Object>(arg0: ToDoubleFunction<T>): Collector<T, any, Number>;

        static averagingInt<T extends Object>(arg0: ToIntFunction<T>): Collector<T, any, Number>;

        static averagingLong<T extends Object>(arg0: ToLongFunction<T>): Collector<T, any, Number>;

        static averagingDouble<T extends Object>(arg0: ToDoubleFunction<T>): Collector<T, any, Number>;

        static reducing<T extends Object>(arg0: T, arg1: BinaryOperator<T>): Collector<T, any, T>;

        static reducing<T extends Object>(arg0: BinaryOperator<T>): Collector<T, any, Optional<T>>;

        static reducing<T extends Object, U extends Object>(arg0: U, arg1: Function<T, U>, arg2: BinaryOperator<U>): Collector<T, any, U>;

        static groupingBy<T extends Object, K extends Object>(arg0: Function<T, K>): Collector<T, any, Map<K, List<T>>>;

        static groupingBy<T extends Object, K extends Object, A extends Object, D extends Object>(arg0: Function<T, K>, arg1: Collector<T, A, D>): Collector<T, any, Map<K, D>>;

        static groupingBy<T extends Object, K extends Object, D extends Object, A extends Object, M extends Map<K, D>>(arg0: Function<T, K>, arg1: Supplier<M>, arg2: Collector<T, A, D>): Collector<T, any, M>;

        static groupingByConcurrent<T extends Object, K extends Object>(arg0: Function<T, K>): Collector<T, any, ConcurrentMap<K, List<T>>>;

        static groupingByConcurrent<T extends Object, K extends Object, A extends Object, D extends Object>(arg0: Function<T, K>, arg1: Collector<T, A, D>): Collector<T, any, ConcurrentMap<K, D>>;

        static groupingByConcurrent<T extends Object, K extends Object, A extends Object, D extends Object, M extends ConcurrentMap<K, D>>(arg0: Function<T, K>, arg1: Supplier<M>, arg2: Collector<T, A, D>): Collector<T, any, M>;

        static partitioningBy<T extends Object>(arg0: Predicate<T>): Collector<T, any, Map<Boolean, List<T>>>;

        static partitioningBy<T extends Object, D extends Object, A extends Object>(arg0: Predicate<T>, arg1: Collector<T, A, D>): Collector<T, any, Map<Boolean, D>>;

        static toMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>): Collector<T, any, Map<K, U>>;

        static toUnmodifiableMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>): Collector<T, any, Map<K, U>>;

        static toMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>, arg2: BinaryOperator<U>): Collector<T, any, Map<K, U>>;

        static toUnmodifiableMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>, arg2: BinaryOperator<U>): Collector<T, any, Map<K, U>>;

        static toMap<T extends Object, K extends Object, U extends Object, M extends Map<K, U>>(arg0: Function<T, K>, arg1: Function<T, U>, arg2: BinaryOperator<U>, arg3: Supplier<M>): Collector<T, any, M>;

        static toConcurrentMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>): Collector<T, any, ConcurrentMap<K, U>>;

        static toConcurrentMap<T extends Object, K extends Object, U extends Object>(arg0: Function<T, K>, arg1: Function<T, U>, arg2: BinaryOperator<U>): Collector<T, any, ConcurrentMap<K, U>>;

        static toConcurrentMap<T extends Object, K extends Object, U extends Object, M extends ConcurrentMap<K, U>>(arg0: Function<T, K>, arg1: Function<T, U>, arg2: BinaryOperator<U>, arg3: Supplier<M>): Collector<T, any, M>;

        static summarizingInt<T extends Object>(arg0: ToIntFunction<T>): Collector<T, any, IntSummaryStatistics>;

        static summarizingLong<T extends Object>(arg0: ToLongFunction<T>): Collector<T, any, LongSummaryStatistics>;

        static summarizingDouble<T extends Object>(arg0: ToDoubleFunction<T>): Collector<T, any, DoubleSummaryStatistics>;

        static teeing<T extends Object, R1 extends Object, R2 extends Object, R extends Object>(arg0: Collector<T, any, R1>, arg1: Collector<T, any, R2>, arg2: BiFunction<R1, R2, R>): Collector<T, any, R>;
    }

    export namespace DoubleStream {
        function
/* default */ builder(): DoubleStream.Builder;
        function
/* default */ empty(): DoubleStream;
        function
/* default */ of(arg0: number): DoubleStream;
        function
/* default */ of(arg0: number[]): DoubleStream;
        function
/* default */ iterate(arg0: number, arg1: DoubleUnaryOperator): DoubleStream;
        function
/* default */ iterate(arg0: number, arg1: DoublePredicate, arg2: DoubleUnaryOperator): DoubleStream;
        function
/* default */ generate(arg0: DoubleSupplier): DoubleStream;
        function
/* default */ concat(arg0: DoubleStream, arg1: DoubleStream): DoubleStream;
    }

    export interface DoubleStream extends BaseStream<Number, DoubleStream>, Object {

        filter(arg0: DoublePredicate): DoubleStream;

        map(arg0: DoubleUnaryOperator): DoubleStream;

        mapToObj<U extends Object>(arg0: DoubleFunction<U>): Stream<U>;

        mapToInt(arg0: DoubleToIntFunction): IntStream;

        mapToLong(arg0: DoubleToLongFunction): LongStream;

        flatMap(arg0: DoubleFunction<DoubleStream>): DoubleStream;

/* default */ mapMulti(arg0: DoubleStream.DoubleMapMultiConsumer): DoubleStream;

        distinct(): DoubleStream;

        sorted(): DoubleStream;

        peek(arg0: DoubleConsumer): DoubleStream;

        limit(arg0: number): DoubleStream;

        skip(arg0: number): DoubleStream;

/* default */ takeWhile(arg0: DoublePredicate): DoubleStream;

/* default */ dropWhile(arg0: DoublePredicate): DoubleStream;

        forEach(arg0: DoubleConsumer): void;

        forEachOrdered(arg0: DoubleConsumer): void;

        toArray(): number[];

        reduce(arg0: number, arg1: DoubleBinaryOperator): number;

        reduce(arg0: DoubleBinaryOperator): OptionalDouble;

        collect<R extends Object>(arg0: Supplier<R>, arg1: ObjDoubleConsumer<R>, arg2: BiConsumer<R, R>): R;

        sum(): number;

        min(): OptionalDouble;

        max(): OptionalDouble;

        count(): number;

        average(): OptionalDouble;

        summaryStatistics(): DoubleSummaryStatistics;

        anyMatch(arg0: DoublePredicate): boolean;

        allMatch(arg0: DoublePredicate): boolean;

        noneMatch(arg0: DoublePredicate): boolean;

        findFirst(): OptionalDouble;

        findAny(): OptionalDouble;

        boxed(): Stream<Number>;

        sequential(): DoubleStream;

        parallel(): DoubleStream;

        iterator(): PrimitiveIterator.OfDouble;

        spliterator(): Spliterator.OfDouble;
    }
    export namespace DoubleStream {
        export interface Builder extends DoubleConsumer {

            accept(arg0: number): void;

/* default */ add(arg0: number): DoubleStream.Builder;

            build(): DoubleStream;
        }

        export interface DoubleMapMultiConsumer {

            accept(arg0: number, arg1: DoubleConsumer): void;
        }

    }

    export namespace IntStream {
        function
/* default */ builder(): IntStream.Builder;
        function
/* default */ empty(): IntStream;
        function
/* default */ of(arg0: number): IntStream;
        function
/* default */ of(arg0: number[]): IntStream;
        function
/* default */ iterate(arg0: number, arg1: IntUnaryOperator): IntStream;
        function
/* default */ iterate(arg0: number, arg1: IntPredicate, arg2: IntUnaryOperator): IntStream;
        function
/* default */ generate(arg0: IntSupplier): IntStream;
        function
/* default */ range(arg0: number, arg1: number): IntStream;
        function
/* default */ rangeClosed(arg0: number, arg1: number): IntStream;
        function
/* default */ concat(arg0: IntStream, arg1: IntStream): IntStream;
    }

    export interface IntStream extends BaseStream<Number, IntStream>, Object {

        filter(arg0: IntPredicate): IntStream;

        map(arg0: IntUnaryOperator): IntStream;

        mapToObj<U extends Object>(arg0: IntFunction<U>): Stream<U>;

        mapToLong(arg0: IntToLongFunction): LongStream;

        mapToDouble(arg0: IntToDoubleFunction): DoubleStream;

        flatMap(arg0: IntFunction<IntStream>): IntStream;

/* default */ mapMulti(arg0: IntStream.IntMapMultiConsumer): IntStream;

        distinct(): IntStream;

        sorted(): IntStream;

        peek(arg0: IntConsumer): IntStream;

        limit(arg0: number): IntStream;

        skip(arg0: number): IntStream;

/* default */ takeWhile(arg0: IntPredicate): IntStream;

/* default */ dropWhile(arg0: IntPredicate): IntStream;

        forEach(arg0: IntConsumer): void;

        forEachOrdered(arg0: IntConsumer): void;

        toArray(): number[];

        reduce(arg0: number, arg1: IntBinaryOperator): number;

        reduce(arg0: IntBinaryOperator): OptionalInt;

        collect<R extends Object>(arg0: Supplier<R>, arg1: ObjIntConsumer<R>, arg2: BiConsumer<R, R>): R;

        sum(): number;

        min(): OptionalInt;

        max(): OptionalInt;

        count(): number;

        average(): OptionalDouble;

        summaryStatistics(): IntSummaryStatistics;

        anyMatch(arg0: IntPredicate): boolean;

        allMatch(arg0: IntPredicate): boolean;

        noneMatch(arg0: IntPredicate): boolean;

        findFirst(): OptionalInt;

        findAny(): OptionalInt;

        asLongStream(): LongStream;

        asDoubleStream(): DoubleStream;

        boxed(): Stream<Number>;

        sequential(): IntStream;

        parallel(): IntStream;

        iterator(): PrimitiveIterator.OfInt;

        spliterator(): Spliterator.OfInt;
    }
    export namespace IntStream {
        export interface Builder extends IntConsumer {

            accept(arg0: number): void;

/* default */ add(arg0: number): IntStream.Builder;

            build(): IntStream;
        }

        export interface IntMapMultiConsumer {

            accept(arg0: number, arg1: IntConsumer): void;
        }

    }

    export namespace LongStream {
        function
/* default */ builder(): LongStream.Builder;
        function
/* default */ empty(): LongStream;
        function
/* default */ of(arg0: number): LongStream;
        function
/* default */ of(arg0: number[]): LongStream;
        function
/* default */ iterate(arg0: number, arg1: LongUnaryOperator): LongStream;
        function
/* default */ iterate(arg0: number, arg1: LongPredicate, arg2: LongUnaryOperator): LongStream;
        function
/* default */ generate(arg0: LongSupplier): LongStream;
        function
/* default */ range(arg0: number, arg1: number): LongStream;
        function
/* default */ rangeClosed(arg0: number, arg1: number): LongStream;
        function
/* default */ concat(arg0: LongStream, arg1: LongStream): LongStream;
    }

    export interface LongStream extends BaseStream<Number, LongStream>, Object {

        filter(arg0: LongPredicate): LongStream;

        map(arg0: LongUnaryOperator): LongStream;

        mapToObj<U extends Object>(arg0: LongFunction<U>): Stream<U>;

        mapToInt(arg0: LongToIntFunction): IntStream;

        mapToDouble(arg0: LongToDoubleFunction): DoubleStream;

        flatMap(arg0: LongFunction<LongStream>): LongStream;

/* default */ mapMulti(arg0: LongStream.LongMapMultiConsumer): LongStream;

        distinct(): LongStream;

        sorted(): LongStream;

        peek(arg0: LongConsumer): LongStream;

        limit(arg0: number): LongStream;

        skip(arg0: number): LongStream;

/* default */ takeWhile(arg0: LongPredicate): LongStream;

/* default */ dropWhile(arg0: LongPredicate): LongStream;

        forEach(arg0: LongConsumer): void;

        forEachOrdered(arg0: LongConsumer): void;

        toArray(): number[];

        reduce(arg0: number, arg1: LongBinaryOperator): number;

        reduce(arg0: LongBinaryOperator): OptionalLong;

        collect<R extends Object>(arg0: Supplier<R>, arg1: ObjLongConsumer<R>, arg2: BiConsumer<R, R>): R;

        sum(): number;

        min(): OptionalLong;

        max(): OptionalLong;

        count(): number;

        average(): OptionalDouble;

        summaryStatistics(): LongSummaryStatistics;

        anyMatch(arg0: LongPredicate): boolean;

        allMatch(arg0: LongPredicate): boolean;

        noneMatch(arg0: LongPredicate): boolean;

        findFirst(): OptionalLong;

        findAny(): OptionalLong;

        asDoubleStream(): DoubleStream;

        boxed(): Stream<Number>;

        sequential(): LongStream;

        parallel(): LongStream;

        iterator(): PrimitiveIterator.OfLong;

        spliterator(): Spliterator.OfLong;
    }
    export namespace LongStream {
        export interface Builder extends LongConsumer {

            accept(arg0: number): void;

/* default */ add(arg0: number): LongStream.Builder;

            build(): LongStream;
        }

        export interface LongMapMultiConsumer {

            accept(arg0: number, arg1: LongConsumer): void;
        }

    }

    export namespace Stream {
        function
/* default */ builder<T extends Object>(): Stream.Builder<T>;
        function
/* default */ empty<T extends Object>(): Stream<T>;
        function
/* default */ of<T extends Object>(arg0: T): Stream<T>;
        function
/* default */ ofNullable<T extends Object>(arg0: T): Stream<T>;
        function
/* default */ of<T extends Object>(arg0: T[]): Stream<T>;
        function
/* default */ iterate<T extends Object>(arg0: T, arg1: UnaryOperator<T>): Stream<T>;
        function
/* default */ iterate<T extends Object>(arg0: T, arg1: Predicate<T>, arg2: UnaryOperator<T>): Stream<T>;
        function
/* default */ generate<T extends Object>(arg0: Supplier<T>): Stream<T>;
        function
/* default */ concat<T extends Object>(arg0: Stream<T>, arg1: Stream<T>): Stream<T>;
    }

    export interface Stream<T extends Object> extends BaseStream<T, Stream<T>>, Object {

        filter(arg0: Predicate<T>): Stream<T>;

        map<R extends Object>(arg0: Function<T, R>): Stream<R>;

        mapToInt(arg0: ToIntFunction<T>): IntStream;

        mapToLong(arg0: ToLongFunction<T>): LongStream;

        mapToDouble(arg0: ToDoubleFunction<T>): DoubleStream;

        flatMap<R extends Object>(arg0: Function<T, Stream<R>>): Stream<R>;

        flatMapToInt(arg0: Function<T, IntStream>): IntStream;

        flatMapToLong(arg0: Function<T, LongStream>): LongStream;

        flatMapToDouble(arg0: Function<T, DoubleStream>): DoubleStream;

/* default */ mapMulti<R extends Object>(arg0: BiConsumer<T, Consumer<R>>): Stream<R>;

/* default */ mapMultiToInt(arg0: BiConsumer<T, IntConsumer>): IntStream;

/* default */ mapMultiToLong(arg0: BiConsumer<T, LongConsumer>): LongStream;

/* default */ mapMultiToDouble(arg0: BiConsumer<T, DoubleConsumer>): DoubleStream;

        distinct(): Stream<T>;

        sorted(): Stream<T>;

        sorted(arg0: Comparator<T>): Stream<T>;

        peek(arg0: Consumer<T>): Stream<T>;

        limit(arg0: number): Stream<T>;

        skip(arg0: number): Stream<T>;

/* default */ takeWhile(arg0: Predicate<T>): Stream<T>;

/* default */ dropWhile(arg0: Predicate<T>): Stream<T>;

        forEach(arg0: Consumer<T>): void;

        forEachOrdered(arg0: Consumer<T>): void;

        toArray(): Object[];

        toArray<A extends Object>(arg0: IntFunction<A[]>): A[];

        reduce(arg0: T, arg1: BinaryOperator<T>): T;

        reduce(arg0: BinaryOperator<T>): Optional<T>;

        reduce<U extends Object>(arg0: U, arg1: BiFunction<U, T, U>, arg2: BinaryOperator<U>): U;

        collect<R extends Object>(arg0: Supplier<R>, arg1: BiConsumer<R, T>, arg2: BiConsumer<R, R>): R;

        collect<R extends Object, A extends Object>(arg0: Collector<T, A, R>): R;

/* default */ toList(): List<T>;

        min(arg0: Comparator<T>): Optional<T>;

        max(arg0: Comparator<T>): Optional<T>;

        count(): number;

        anyMatch(arg0: Predicate<T>): boolean;

        allMatch(arg0: Predicate<T>): boolean;

        noneMatch(arg0: Predicate<T>): boolean;

        findFirst(): Optional<T>;

        findAny(): Optional<T>;
    }
    export namespace Stream {
        export interface Builder<T extends Object> extends Consumer<T>, Object {

            accept(arg0: T): void;

/* default */ add(arg0: T): Stream.Builder<T>;

            build(): Stream<T>;
        }

    }

    export class StreamSupport {

        static stream<T extends Object>(arg0: Spliterator<T>, arg1: boolean): Stream<T>;

        static stream<T extends Object>(arg0: Supplier<Spliterator<T>>, arg1: number, arg2: boolean): Stream<T>;

        static intStream(arg0: Spliterator.OfInt, arg1: boolean): IntStream;

        static intStream(arg0: Supplier<Spliterator.OfInt>, arg1: number, arg2: boolean): IntStream;

        static longStream(arg0: Spliterator.OfLong, arg1: boolean): LongStream;

        static longStream(arg0: Supplier<Spliterator.OfLong>, arg1: number, arg2: boolean): LongStream;

        static doubleStream(arg0: Spliterator.OfDouble, arg1: boolean): DoubleStream;

        static doubleStream(arg0: Supplier<Spliterator.OfDouble>, arg1: number, arg2: boolean): DoubleStream;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.util.spi' {
    import { Integer, String } from '@java/java.lang'
    import { Locale, ResourceBundle, Optional, Map } from '@java/java.util'
    import { PrintStream, PrintWriter } from '@java/java.io'
    export abstract class AbstractResourceBundleProvider implements ResourceBundleProvider {

        getBundle(arg0: String, arg1: Locale): ResourceBundle;
    }

    export abstract class CalendarDataProvider extends LocaleServiceProvider {

        abstract getFirstDayOfWeek(arg0: Locale): number;

        abstract getMinimalDaysInFirstWeek(arg0: Locale): number;
    }

    export abstract class CalendarNameProvider extends LocaleServiceProvider {

        abstract getDisplayName(arg0: String, arg1: number, arg2: number, arg3: number, arg4: Locale): String;

        abstract getDisplayNames(arg0: String, arg1: number, arg2: number, arg3: Locale): Map<String, Number>;
    }

    export abstract class CurrencyNameProvider extends LocaleServiceProvider {

        abstract getSymbol(arg0: String, arg1: Locale): String;

        getDisplayName(arg0: String, arg1: Locale): String;
    }

    export abstract class LocaleNameProvider extends LocaleServiceProvider {

        abstract getDisplayLanguage(arg0: String, arg1: Locale): String;

        getDisplayScript(arg0: String, arg1: Locale): String;

        abstract getDisplayCountry(arg0: String, arg1: Locale): String;

        abstract getDisplayVariant(arg0: String, arg1: Locale): String;

        getDisplayUnicodeExtensionKey(arg0: String, arg1: Locale): String;

        getDisplayUnicodeExtensionType(arg0: String, arg1: String, arg2: Locale): String;
    }

    export abstract class LocaleServiceProvider {

        abstract getAvailableLocales(): Locale[];

        isSupportedLocale(arg0: Locale): boolean;
    }

    export interface ResourceBundleControlProvider {

        getControl(arg0: String): ResourceBundle.Control;
    }

    export interface ResourceBundleProvider {

        getBundle(arg0: String, arg1: Locale): ResourceBundle;
    }

    export abstract class TimeZoneNameProvider extends LocaleServiceProvider {

        abstract getDisplayName(arg0: String, arg1: boolean, arg2: number, arg3: Locale): String;

        getGenericDisplayName(arg0: String, arg1: number, arg2: Locale): String;
    }

    export namespace ToolProvider {
        function
/* default */ findFirst(arg0: String): Optional<ToolProvider>;
    }

    export interface ToolProvider {

        name(): String;

        run(arg0: PrintWriter, arg1: PrintWriter, arg2: String[]): number;

/* default */ run(arg0: PrintStream, arg1: PrintStream, arg2: String[]): number;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.util.regex' {
    import { CharSequence, StringBuffer, String, StringBuilder, IllegalArgumentException } from '@java/java.lang'
    import { Serializable } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { Function, Predicate } from '@java/java.util.function'
    export interface MatchResult {

        start(): number;

        start(arg0: number): number;

        end(): number;

        end(arg0: number): number;

        group(): String;

        group(arg0: number): String;

        groupCount(): number;
    }

    export class Matcher implements MatchResult {

        pattern(): Pattern;

        toMatchResult(): MatchResult;

        usePattern(arg0: Pattern): Matcher;

        reset(): Matcher;

        reset(arg0: CharSequence): Matcher;

        start(): number;

        start(arg0: number): number;

        start(arg0: String): number;

        end(): number;

        end(arg0: number): number;

        end(arg0: String): number;

        group(): String;

        group(arg0: number): String;

        group(arg0: String): String;

        groupCount(): number;

        matches(): boolean;

        find(): boolean;

        find(arg0: number): boolean;

        lookingAt(): boolean;

        static quoteReplacement(arg0: String): String;

        appendReplacement(arg0: StringBuffer, arg1: String): Matcher;

        appendReplacement(arg0: StringBuilder, arg1: String): Matcher;

        appendTail(arg0: StringBuffer): StringBuffer;

        appendTail(arg0: StringBuilder): StringBuilder;

        replaceAll(arg0: String): String;

        replaceAll(arg0: Function<MatchResult, String>): String;

        results(): Stream<MatchResult>;

        replaceFirst(arg0: String): String;

        replaceFirst(arg0: Function<MatchResult, String>): String;

        region(arg0: number, arg1: number): Matcher;

        regionStart(): number;

        regionEnd(): number;

        hasTransparentBounds(): boolean;

        useTransparentBounds(arg0: boolean): Matcher;

        hasAnchoringBounds(): boolean;

        useAnchoringBounds(arg0: boolean): Matcher;
        toString(): string;

        hitEnd(): boolean;

        requireEnd(): boolean;
    }

    export class Pattern implements Serializable {
        static UNIX_LINES: number
        static CASE_INSENSITIVE: number
        static COMMENTS: number
        static MULTILINE: number
        static LITERAL: number
        static DOTALL: number
        static UNICODE_CASE: number
        static CANON_EQ: number
        static UNICODE_CHARACTER_CLASS: number

        static compile(arg0: String): Pattern;

        static compile(arg0: String, arg1: number): Pattern;

        pattern(): String;
        toString(): string;

        matcher(arg0: CharSequence): Matcher;

        flags(): number;

        static matches(arg0: String, arg1: CharSequence): boolean;

        split(arg0: CharSequence, arg1: number): String[];

        split(arg0: CharSequence): String[];

        static quote(arg0: String): String;

        asPredicate(): Predicate<String>;

        asMatchPredicate(): Predicate<String>;

        splitAsStream(arg0: CharSequence): Stream<String>;
    }

    export class PatternSyntaxException extends IllegalArgumentException {
        constructor(arg0: String, arg1: String, arg2: number);

        getIndex(): number;

        getDescription(): String;

        getPattern(): String;

        getMessage(): String;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.math.d.ts" />
declare module '@java/java.util.random' {
    import { String } from '@java/java.lang'
    import { DoubleStream, Stream, LongStream, IntStream } from '@java/java.util.stream'
    import { BigInteger } from '@java/java.math'
    export namespace RandomGenerator {
        function
/* default */ of(arg0: String): RandomGenerator;
        function
/* default */ getDefault(): RandomGenerator;
    }

    export interface RandomGenerator {

/* default */ isDeprecated(): boolean;

/* default */ doubles(): DoubleStream;

/* default */ doubles(arg0: number, arg1: number): DoubleStream;

/* default */ doubles(arg0: number): DoubleStream;

/* default */ doubles(arg0: number, arg1: number, arg2: number): DoubleStream;

/* default */ ints(): IntStream;

/* default */ ints(arg0: number, arg1: number): IntStream;

/* default */ ints(arg0: number): IntStream;

/* default */ ints(arg0: number, arg1: number, arg2: number): IntStream;

/* default */ longs(): LongStream;

/* default */ longs(arg0: number, arg1: number): LongStream;

/* default */ longs(arg0: number): LongStream;

/* default */ longs(arg0: number, arg1: number, arg2: number): LongStream;

/* default */ nextBoolean(): boolean;

/* default */ nextBytes(arg0: number[]): void;

/* default */ nextFloat(): number;

/* default */ nextFloat(arg0: number): number;

/* default */ nextFloat(arg0: number, arg1: number): number;

/* default */ nextDouble(): number;

/* default */ nextDouble(arg0: number): number;

/* default */ nextDouble(arg0: number, arg1: number): number;

/* default */ nextInt(): number;

/* default */ nextInt(arg0: number): number;

/* default */ nextInt(arg0: number, arg1: number): number;

        nextLong(): number;

/* default */ nextLong(arg0: number): number;

/* default */ nextLong(arg0: number, arg1: number): number;

/* default */ nextGaussian(): number;

/* default */ nextGaussian(arg0: number, arg1: number): number;

/* default */ nextExponential(): number;
    }
    export namespace RandomGenerator {
        export namespace ArbitrarilyJumpableGenerator {
            function
/* default */ of(arg0: String): RandomGenerator.ArbitrarilyJumpableGenerator;
        }

        export interface ArbitrarilyJumpableGenerator extends RandomGenerator.LeapableGenerator {

            copy(): RandomGenerator.ArbitrarilyJumpableGenerator;

            jumpPowerOfTwo(arg0: number): void;

            jump(arg0: number): void;

/* default */ jump(): void;

/* default */ jumps(arg0: number): Stream<RandomGenerator.ArbitrarilyJumpableGenerator>;

/* default */ jumps(arg0: number, arg1: number): Stream<RandomGenerator.ArbitrarilyJumpableGenerator>;

/* default */ leap(): void;

/* default */ copyAndJump(arg0: number): RandomGenerator.ArbitrarilyJumpableGenerator;
        }

        export namespace JumpableGenerator {
            function
/* default */ of(arg0: String): RandomGenerator.JumpableGenerator;
        }

        export interface JumpableGenerator extends RandomGenerator.StreamableGenerator {

            copy(): RandomGenerator.JumpableGenerator;

            jump(): void;

            jumpDistance(): number;

/* default */ jumps(): Stream<RandomGenerator>;

/* default */ jumps(arg0: number): Stream<RandomGenerator>;

/* default */ rngs(): Stream<RandomGenerator>;

/* default */ rngs(arg0: number): Stream<RandomGenerator>;

/* default */ copyAndJump(): RandomGenerator;
        }

        export namespace LeapableGenerator {
            function
/* default */ of(arg0: String): RandomGenerator.LeapableGenerator;
        }

        export interface LeapableGenerator extends RandomGenerator.JumpableGenerator {

            copy(): RandomGenerator.LeapableGenerator;

            leap(): void;

            leapDistance(): number;

/* default */ leaps(): Stream<RandomGenerator.JumpableGenerator>;

/* default */ leaps(arg0: number): Stream<RandomGenerator.JumpableGenerator>;

/* default */ copyAndLeap(): RandomGenerator.JumpableGenerator;
        }

        export namespace SplittableGenerator {
            function
/* default */ of(arg0: String): RandomGenerator.SplittableGenerator;
        }

        export interface SplittableGenerator extends RandomGenerator.StreamableGenerator {

            split(): RandomGenerator.SplittableGenerator;

            split(arg0: RandomGenerator.SplittableGenerator): RandomGenerator.SplittableGenerator;

/* default */ splits(): Stream<RandomGenerator.SplittableGenerator>;

            splits(arg0: number): Stream<RandomGenerator.SplittableGenerator>;

            splits(arg0: RandomGenerator.SplittableGenerator): Stream<RandomGenerator.SplittableGenerator>;

            splits(arg0: number, arg1: RandomGenerator.SplittableGenerator): Stream<RandomGenerator.SplittableGenerator>;

/* default */ rngs(): Stream<RandomGenerator>;

/* default */ rngs(arg0: number): Stream<RandomGenerator>;
        }

        export namespace StreamableGenerator {
            function
/* default */ of(arg0: String): RandomGenerator.StreamableGenerator;
        }

        export interface StreamableGenerator extends RandomGenerator {

            rngs(): Stream<RandomGenerator>;

/* default */ rngs(arg0: number): Stream<RandomGenerator>;
        }

    }

    export class RandomGeneratorFactory<T extends RandomGenerator> extends Object {

        static of<T extends RandomGenerator>(arg0: String): RandomGeneratorFactory<T>;

        static getDefault(): RandomGeneratorFactory<RandomGenerator>;

        static all(): Stream<RandomGeneratorFactory<RandomGenerator>>;

        name(): String;

        group(): String;

        stateBits(): number;

        equidistribution(): number;

        period(): BigInteger;

        isStatistical(): boolean;

        isStochastic(): boolean;

        isHardware(): boolean;

        isArbitrarilyJumpable(): boolean;

        isJumpable(): boolean;

        isLeapable(): boolean;

        isSplittable(): boolean;

        isStreamable(): boolean;

        isDeprecated(): boolean;

        create(): T;

        create(arg0: number): T;

        create(arg0: number[]): T;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.util.prefs' {
    import { Throwable, Class, String, Exception } from '@java/java.lang'
    import { EventObject, EventListener } from '@java/java.util'
    import { InputStream, OutputStream } from '@java/java.io'
    export abstract class AbstractPreferences extends Preferences {

        put(arg0: String, arg1: String): void;

        get(arg0: String, arg1: String): String;

        remove(arg0: String): void;

        clear(): void;

        putInt(arg0: String, arg1: number): void;

        getInt(arg0: String, arg1: number): number;

        putLong(arg0: String, arg1: number): void;

        getLong(arg0: String, arg1: number): number;

        putBoolean(arg0: String, arg1: boolean): void;

        getBoolean(arg0: String, arg1: boolean): boolean;

        putFloat(arg0: String, arg1: number): void;

        getFloat(arg0: String, arg1: number): number;

        putDouble(arg0: String, arg1: number): void;

        getDouble(arg0: String, arg1: number): number;

        putByteArray(arg0: String, arg1: number[]): void;

        getByteArray(arg0: String, arg1: number[]): number[];

        keys(): String[];

        childrenNames(): String[];

        parent(): Preferences;

        node(arg0: String): Preferences;

        nodeExists(arg0: String): boolean;

        removeNode(): void;

        name(): String;

        absolutePath(): String;

        isUserNode(): boolean;

        addPreferenceChangeListener(arg0: PreferenceChangeListener): void;

        removePreferenceChangeListener(arg0: PreferenceChangeListener): void;

        addNodeChangeListener(arg0: NodeChangeListener): void;

        removeNodeChangeListener(arg0: NodeChangeListener): void;
        toString(): string;

        sync(): void;

        flush(): void;

        exportNode(arg0: OutputStream): void;

        exportSubtree(arg0: OutputStream): void;
    }

    export class BackingStoreException extends Exception {
        constructor(arg0: String);
        constructor(arg0: Throwable);
    }

    export class InvalidPreferencesFormatException extends Exception {
        constructor(arg0: Throwable);
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export class NodeChangeEvent extends EventObject {
        constructor(arg0: Preferences, arg1: Preferences);

        getParent(): Preferences;

        getChild(): Preferences;
    }

    export interface NodeChangeListener extends EventListener {

        childAdded(arg0: NodeChangeEvent): void;

        childRemoved(arg0: NodeChangeEvent): void;
    }

    export class PreferenceChangeEvent extends EventObject {
        constructor(arg0: Preferences, arg1: String, arg2: String);

        getNode(): Preferences;

        getKey(): String;

        getNewValue(): String;
    }

    export interface PreferenceChangeListener extends EventListener {

        preferenceChange(arg0: PreferenceChangeEvent): void;
    }

    export abstract class Preferences {
        static MAX_KEY_LENGTH: number
        static MAX_VALUE_LENGTH: number
        static MAX_NAME_LENGTH: number

        static userNodeForPackage(arg0: Class<any>): Preferences;

        static systemNodeForPackage(arg0: Class<any>): Preferences;

        static userRoot(): Preferences;

        static systemRoot(): Preferences;

        abstract put(arg0: String, arg1: String): void;

        abstract get(arg0: String, arg1: String): String;

        abstract remove(arg0: String): void;

        abstract clear(): void;

        abstract putInt(arg0: String, arg1: number): void;

        abstract getInt(arg0: String, arg1: number): number;

        abstract putLong(arg0: String, arg1: number): void;

        abstract getLong(arg0: String, arg1: number): number;

        abstract putBoolean(arg0: String, arg1: boolean): void;

        abstract getBoolean(arg0: String, arg1: boolean): boolean;

        abstract putFloat(arg0: String, arg1: number): void;

        abstract getFloat(arg0: String, arg1: number): number;

        abstract putDouble(arg0: String, arg1: number): void;

        abstract getDouble(arg0: String, arg1: number): number;

        abstract putByteArray(arg0: String, arg1: number[]): void;

        abstract getByteArray(arg0: String, arg1: number[]): number[];

        abstract keys(): String[];

        abstract childrenNames(): String[];

        abstract parent(): Preferences;

        abstract node(arg0: String): Preferences;

        abstract nodeExists(arg0: String): boolean;

        abstract removeNode(): void;

        abstract name(): String;

        abstract absolutePath(): String;

        abstract isUserNode(): boolean;
        toString(): string;

        abstract flush(): void;

        abstract sync(): void;

        abstract addPreferenceChangeListener(arg0: PreferenceChangeListener): void;

        abstract removePreferenceChangeListener(arg0: PreferenceChangeListener): void;

        abstract addNodeChangeListener(arg0: NodeChangeListener): void;

        abstract removeNodeChangeListener(arg0: NodeChangeListener): void;

        abstract exportNode(arg0: OutputStream): void;

        abstract exportSubtree(arg0: OutputStream): void;

        static importPreferences(arg0: InputStream): void;
    }

    export interface PreferencesFactory {

        systemRoot(): Preferences;

        userRoot(): Preferences;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.util.logging' {
    import { BasicPermission } from '@java/java.security'
    import { Throwable, String, Exception, Runnable } from '@java/java.lang'
    import { ResourceBundle, List, Enumeration } from '@java/java.util'
    import { Instant } from '@java/java.time'
    import { Serializable, InputStream, OutputStream } from '@java/java.io'
    import { Function, BiFunction, Supplier } from '@java/java.util.function'
    export class ConsoleHandler extends StreamHandler {
        constructor();

        publish(arg0: LogRecord): void;

        close(): void;
    }

    export class ErrorManager {
        static GENERIC_FAILURE: number
        static WRITE_FAILURE: number
        static FLUSH_FAILURE: number
        static CLOSE_FAILURE: number
        static OPEN_FAILURE: number
        static FORMAT_FAILURE: number
        constructor();

        error(arg0: String, arg1: Exception, arg2: number): void;
    }

    export class FileHandler extends StreamHandler {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: boolean);
        constructor(arg0: String, arg1: number, arg2: number);
        constructor(arg0: String, arg1: number, arg2: number, arg3: boolean);
        constructor(arg0: String, arg1: number, arg2: number, arg3: boolean);

        publish(arg0: LogRecord): void;

        close(): void;
    }

    export interface Filter {

        isLoggable(arg0: LogRecord): boolean;
    }

    export abstract class Formatter {

        abstract format(arg0: LogRecord): String;

        getHead(arg0: Handler): String;

        getTail(arg0: Handler): String;

        formatMessage(arg0: LogRecord): String;
    }

    export abstract class Handler {

        abstract publish(arg0: LogRecord): void;

        abstract flush(): void;

        abstract close(): void;

        setFormatter(arg0: Formatter): void;

        getFormatter(): Formatter;

        setEncoding(arg0: String): void;

        getEncoding(): String;

        setFilter(arg0: Filter): void;

        getFilter(): Filter;

        setErrorManager(arg0: ErrorManager): void;

        getErrorManager(): ErrorManager;

        setLevel(arg0: Level): void;

        getLevel(): Level;

        isLoggable(arg0: LogRecord): boolean;
    }

    export class Level implements Serializable {
        static OFF: Level
        static SEVERE: Level
        static WARNING: Level
        static INFO: Level
        static CONFIG: Level
        static FINE: Level
        static FINER: Level
        static FINEST: Level
        static ALL: Level

        getResourceBundleName(): String;

        getName(): String;

        getLocalizedName(): String;
        toString(): string;

        intValue(): number;

        static parse(arg0: String): Level;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class LogManager {
        static LOGGING_MXBEAN_NAME: String

        static getLogManager(): LogManager;

        addLogger(arg0: Logger): boolean;

        getLogger(arg0: String): Logger;

        getLoggerNames(): Enumeration<String>;

        readConfiguration(): void;

        reset(): void;

        readConfiguration(arg0: InputStream): void;

        updateConfiguration(arg0: Function<String, BiFunction<String, String, String>>): void;

        updateConfiguration(arg0: InputStream, arg1: Function<String, BiFunction<String, String, String>>): void;

        getProperty(arg0: String): String;

        checkAccess(): void;

        static getLoggingMXBean(): LoggingMXBean;

        addConfigurationListener(arg0: Runnable): LogManager;

        removeConfigurationListener(arg0: Runnable): void;
    }

    export class LogRecord implements Serializable {
        constructor(arg0: Level, arg1: String);

        getLoggerName(): String;

        setLoggerName(arg0: String): void;

        getResourceBundle(): ResourceBundle;

        setResourceBundle(arg0: ResourceBundle): void;

        getResourceBundleName(): String;

        setResourceBundleName(arg0: String): void;

        getLevel(): Level;

        setLevel(arg0: Level): void;

        getSequenceNumber(): number;

        setSequenceNumber(arg0: number): void;

        getSourceClassName(): String;

        setSourceClassName(arg0: String): void;

        getSourceMethodName(): String;

        setSourceMethodName(arg0: String): void;

        getMessage(): String;

        setMessage(arg0: String): void;

        getParameters(): Object[];

        setParameters(arg0: Object[]): void;

        getThreadID(): number;

        setThreadID(arg0: number): void;

        getLongThreadID(): number;

        setLongThreadID(arg0: number): LogRecord;

        getMillis(): number;

        setMillis(arg0: number): void;

        getInstant(): Instant;

        setInstant(arg0: Instant): void;

        getThrown(): Throwable;

        setThrown(arg0: Throwable): void;
    }

    export class Logger {
        static GLOBAL_LOGGER_NAME: String
        static global: Logger

        static getGlobal(): Logger;

        static getLogger(arg0: String): Logger;

        static getLogger(arg0: String, arg1: String): Logger;

        static getAnonymousLogger(): Logger;

        static getAnonymousLogger(arg0: String): Logger;

        getResourceBundle(): ResourceBundle;

        getResourceBundleName(): String;

        setFilter(arg0: Filter): void;

        getFilter(): Filter;

        log(arg0: LogRecord): void;

        log(arg0: Level, arg1: String): void;

        log(arg0: Level, arg1: Supplier<String>): void;

        log(arg0: Level, arg1: String, arg2: Object): void;

        log(arg0: Level, arg1: String, arg2: Object[]): void;

        log(arg0: Level, arg1: String, arg2: Throwable): void;

        log(arg0: Level, arg1: Throwable, arg2: Supplier<String>): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: String): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: Supplier<String>): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: Object): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: Object[]): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: Throwable): void;

        logp(arg0: Level, arg1: String, arg2: String, arg3: Throwable, arg4: Supplier<String>): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: String): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: String, arg5: Object): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: String, arg5: Object[]): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: ResourceBundle, arg4: String, arg5: Object[]): void;

        logrb(arg0: Level, arg1: ResourceBundle, arg2: String, arg3: Object[]): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: String, arg4: String, arg5: Throwable): void;

        logrb(arg0: Level, arg1: String, arg2: String, arg3: ResourceBundle, arg4: String, arg5: Throwable): void;

        logrb(arg0: Level, arg1: ResourceBundle, arg2: String, arg3: Throwable): void;

        entering(arg0: String, arg1: String): void;

        entering(arg0: String, arg1: String, arg2: Object): void;

        entering(arg0: String, arg1: String, arg2: Object[]): void;

        exiting(arg0: String, arg1: String): void;

        exiting(arg0: String, arg1: String, arg2: Object): void;

        throwing(arg0: String, arg1: String, arg2: Throwable): void;

        severe(arg0: String): void;

        warning(arg0: String): void;

        info(arg0: String): void;

        config(arg0: String): void;

        fine(arg0: String): void;

        finer(arg0: String): void;

        finest(arg0: String): void;

        severe(arg0: Supplier<String>): void;

        warning(arg0: Supplier<String>): void;

        info(arg0: Supplier<String>): void;

        config(arg0: Supplier<String>): void;

        fine(arg0: Supplier<String>): void;

        finer(arg0: Supplier<String>): void;

        finest(arg0: Supplier<String>): void;

        setLevel(arg0: Level): void;

        getLevel(): Level;

        isLoggable(arg0: Level): boolean;

        getName(): String;

        addHandler(arg0: Handler): void;

        removeHandler(arg0: Handler): void;

        getHandlers(): Handler[];

        setUseParentHandlers(arg0: boolean): void;

        getUseParentHandlers(): boolean;

        setResourceBundle(arg0: ResourceBundle): void;

        getParent(): Logger;

        setParent(arg0: Logger): void;
    }

    export interface LoggingMXBean {

        getLoggerNames(): List<String>;

        getLoggerLevel(arg0: String): String;

        setLoggerLevel(arg0: String, arg1: String): void;

        getParentLoggerName(arg0: String): String;
    }

    export class LoggingPermission extends BasicPermission {
        constructor(arg0: String, arg1: String);
    }

    export class MemoryHandler extends Handler {
        constructor();
        constructor(arg0: Handler, arg1: number, arg2: Level);

        publish(arg0: LogRecord): void;

        push(): void;

        flush(): void;

        close(): void;

        setPushLevel(arg0: Level): void;

        getPushLevel(): Level;

        isLoggable(arg0: LogRecord): boolean;
    }

    export class SimpleFormatter extends Formatter {
        constructor();

        format(arg0: LogRecord): String;
    }

    export class SocketHandler extends StreamHandler {
        constructor();
        constructor(arg0: String, arg1: number);

        close(): void;

        publish(arg0: LogRecord): void;
    }

    export class StreamHandler extends Handler {
        constructor();
        constructor(arg0: OutputStream, arg1: Formatter);

        setEncoding(arg0: String): void;

        publish(arg0: LogRecord): void;

        isLoggable(arg0: LogRecord): boolean;

        flush(): void;

        close(): void;
    }

    export class XMLFormatter extends Formatter {
        constructor();

        format(arg0: LogRecord): String;

        getHead(arg0: Handler): String;

        getTail(arg0: Handler): String;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.zip.d.ts" />
/// <reference path="java.security.cert.d.ts" />
declare module '@java/java.util.jar' {
    import { CodeSigner } from '@java/java.security'
    import { Collection, Set, Enumeration, Map } from '@java/java.util'
    import { Cloneable, Runtime, String } from '@java/java.lang'
    import { File, InputStream, OutputStream } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { ZipException, ZipInputStream, ZipFile, ZipOutputStream, ZipEntry } from '@java/java.util.zip'
    import { Certificate } from '@java/java.security.cert'
    export interface Attributes extends Map<Object, Object>, Cloneable { }
    export class Attributes extends Object implements Map<Object, Object>, Cloneable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Attributes);

        get(arg0: Object): Object;

        getValue(arg0: String): String;

        getValue(arg0: Attributes.Name): String;

        put(arg0: Object, arg1: Object): Object;

        putValue(arg0: String, arg1: String): String;

        remove(arg0: Object): Object;

        containsValue(arg0: Object): boolean;

        containsKey(arg0: Object): boolean;

        putAll(arg0: Map<any, any>): void;

        clear(): void;

        size(): number;

        isEmpty(): boolean;

        keySet(): Set<Object>;

        values(): Collection<Object>;

        entrySet(): Set<Map.Entry<Object, Object>>;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): Object;
    }
    export namespace Attributes {
        export class Name {
            static MANIFEST_VERSION: Attributes.Name
            static SIGNATURE_VERSION: Attributes.Name
            static CONTENT_TYPE: Attributes.Name
            static CLASS_PATH: Attributes.Name
            static MAIN_CLASS: Attributes.Name
            static SEALED: Attributes.Name
            static EXTENSION_LIST: Attributes.Name
            static EXTENSION_NAME: Attributes.Name
            static EXTENSION_INSTALLATION: Attributes.Name
            static IMPLEMENTATION_TITLE: Attributes.Name
            static IMPLEMENTATION_VERSION: Attributes.Name
            static IMPLEMENTATION_VENDOR: Attributes.Name
            static IMPLEMENTATION_VENDOR_ID: Attributes.Name
            static IMPLEMENTATION_URL: Attributes.Name
            static SPECIFICATION_TITLE: Attributes.Name
            static SPECIFICATION_VERSION: Attributes.Name
            static SPECIFICATION_VENDOR: Attributes.Name
            static MULTI_RELEASE: Attributes.Name
            constructor(arg0: String);

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

    }

    export class JarEntry extends ZipEntry {
        constructor(arg0: String);
        constructor(arg0: ZipEntry);
        constructor(arg0: JarEntry);

        getAttributes(): Attributes;

        getCertificates(): Certificate[];

        getCodeSigners(): CodeSigner[];

        getRealName(): String;
    }

    export class JarException extends ZipException {
        constructor();
        constructor(arg0: String);
    }

    export class JarFile extends ZipFile {
        static MANIFEST_NAME: String
        constructor(arg0: String);
        constructor(arg0: String, arg1: boolean);
        constructor(arg0: File);
        constructor(arg0: File, arg1: boolean);
        constructor(arg0: File, arg1: boolean, arg2: number);
        constructor(arg0: File, arg1: boolean, arg2: number, arg3: Runtime.Version);

        static baseVersion(): Runtime.Version;

        static runtimeVersion(): Runtime.Version;

        getVersion(): Runtime.Version;

        isMultiRelease(): boolean;

        getManifest(): Manifest;

        getJarEntry(arg0: String): JarEntry;

        getEntry(arg0: String): ZipEntry;

        entries(): Enumeration<JarEntry>;

        stream(): Stream<JarEntry>;

        versionedStream(): Stream<JarEntry>;

        getInputStream(arg0: ZipEntry): InputStream;
    }

    export class JarInputStream extends ZipInputStream {
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: boolean);

        getManifest(): Manifest;

        getNextEntry(): ZipEntry;

        getNextJarEntry(): JarEntry;

        read(arg0: number[], arg1: number, arg2: number): number;
    }

    export class JarOutputStream extends ZipOutputStream {
        constructor(arg0: OutputStream, arg1: Manifest);
        constructor(arg0: OutputStream);

        putNextEntry(arg0: ZipEntry): void;
    }

    export class Manifest implements Cloneable {
        constructor();
        constructor(arg0: InputStream);
        constructor(arg0: Manifest);

        getMainAttributes(): Attributes;

        getEntries(): Map<String, Attributes>;

        getAttributes(arg0: String): Attributes;

        clear(): void;

        write(arg0: OutputStream): void;

        read(arg0: InputStream): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): Object;
    }

}
/// <reference path="java.util.d.ts" />
declare module '@java/java.util.function' {
    import { Comparator } from '@java/java.util'
    export interface BiConsumer<T extends Object, U extends Object> extends Object {

        accept(arg0: T, arg1: U): void;

/* default */ andThen(arg0: BiConsumer<T, U>): BiConsumer<T, U>;
    }

    export interface BiFunction<T extends Object, U extends Object, R extends Object> extends Object {

        apply(arg0: T, arg1: U): R;

/* default */ andThen<V extends Object>(arg0: Function<R, V>): BiFunction<T, U, V>;
    }

    export interface BiPredicate<T extends Object, U extends Object> extends Object {

        test(arg0: T, arg1: U): boolean;

/* default */ and(arg0: BiPredicate<T, U>): BiPredicate<T, U>;

/* default */ negate(): BiPredicate<T, U>;

/* default */ or(arg0: BiPredicate<T, U>): BiPredicate<T, U>;
    }

    export namespace BinaryOperator {
        function
/* default */ minBy<T extends Object>(arg0: Comparator<T>): BinaryOperator<T>;
        function
/* default */ maxBy<T extends Object>(arg0: Comparator<T>): BinaryOperator<T>;
    }

    export interface BinaryOperator<T extends Object> extends BiFunction<T, T, T>, Object {
    }

    export interface BooleanSupplier {

        getAsBoolean(): boolean;
    }

    export interface Consumer<T extends Object> extends Object {

        accept(arg0: T): void;

/* default */ andThen(arg0: Consumer<T>): Consumer<T>;
    }

    export interface DoubleBinaryOperator {

        applyAsDouble(arg0: number, arg1: number): number;
    }

    export interface DoubleConsumer {

        accept(arg0: number): void;

/* default */ andThen(arg0: DoubleConsumer): DoubleConsumer;
    }

    export interface DoubleFunction<R extends Object> extends Object {

        apply(arg0: number): R;
    }

    export interface DoublePredicate {

        test(arg0: number): boolean;

/* default */ and(arg0: DoublePredicate): DoublePredicate;

/* default */ negate(): DoublePredicate;

/* default */ or(arg0: DoublePredicate): DoublePredicate;
    }

    export interface DoubleSupplier {

        getAsDouble(): number;
    }

    export interface DoubleToIntFunction {

        applyAsInt(arg0: number): number;
    }

    export interface DoubleToLongFunction {

        applyAsLong(arg0: number): number;
    }

    export namespace DoubleUnaryOperator {
        function
/* default */ identity(): DoubleUnaryOperator;
    }

    export interface DoubleUnaryOperator {

        applyAsDouble(arg0: number): number;

/* default */ compose(arg0: DoubleUnaryOperator): DoubleUnaryOperator;

/* default */ andThen(arg0: DoubleUnaryOperator): DoubleUnaryOperator;
    }

    export namespace Function {
        function
/* default */ identity<T extends Object>(): Function<T, T>;
    }

    export interface Function<T extends Object, R extends Object> extends Object {

        apply(arg0: T): R;

/* default */ compose<V extends Object>(arg0: Function<V, T>): Function<V, R>;

/* default */ andThen<V extends Object>(arg0: Function<R, V>): Function<T, V>;
    }

    export interface IntBinaryOperator {

        applyAsInt(arg0: number, arg1: number): number;
    }

    export interface IntConsumer {

        accept(arg0: number): void;

/* default */ andThen(arg0: IntConsumer): IntConsumer;
    }

    export interface IntFunction<R extends Object> extends Object {

        apply(arg0: number): R;
    }

    export interface IntPredicate {

        test(arg0: number): boolean;

/* default */ and(arg0: IntPredicate): IntPredicate;

/* default */ negate(): IntPredicate;

/* default */ or(arg0: IntPredicate): IntPredicate;
    }

    export interface IntSupplier {

        getAsInt(): number;
    }

    export interface IntToDoubleFunction {

        applyAsDouble(arg0: number): number;
    }

    export interface IntToLongFunction {

        applyAsLong(arg0: number): number;
    }

    export namespace IntUnaryOperator {
        function
/* default */ identity(): IntUnaryOperator;
    }

    export interface IntUnaryOperator {

        applyAsInt(arg0: number): number;

/* default */ compose(arg0: IntUnaryOperator): IntUnaryOperator;

/* default */ andThen(arg0: IntUnaryOperator): IntUnaryOperator;
    }

    export interface LongBinaryOperator {

        applyAsLong(arg0: number, arg1: number): number;
    }

    export interface LongConsumer {

        accept(arg0: number): void;

/* default */ andThen(arg0: LongConsumer): LongConsumer;
    }

    export interface LongFunction<R extends Object> extends Object {

        apply(arg0: number): R;
    }

    export interface LongPredicate {

        test(arg0: number): boolean;

/* default */ and(arg0: LongPredicate): LongPredicate;

/* default */ negate(): LongPredicate;

/* default */ or(arg0: LongPredicate): LongPredicate;
    }

    export interface LongSupplier {

        getAsLong(): number;
    }

    export interface LongToDoubleFunction {

        applyAsDouble(arg0: number): number;
    }

    export interface LongToIntFunction {

        applyAsInt(arg0: number): number;
    }

    export namespace LongUnaryOperator {
        function
/* default */ identity(): LongUnaryOperator;
    }

    export interface LongUnaryOperator {

        applyAsLong(arg0: number): number;

/* default */ compose(arg0: LongUnaryOperator): LongUnaryOperator;

/* default */ andThen(arg0: LongUnaryOperator): LongUnaryOperator;
    }

    export interface ObjDoubleConsumer<T extends Object> extends Object {

        accept(arg0: T, arg1: number): void;
    }

    export interface ObjIntConsumer<T extends Object> extends Object {

        accept(arg0: T, arg1: number): void;
    }

    export interface ObjLongConsumer<T extends Object> extends Object {

        accept(arg0: T, arg1: number): void;
    }

    export namespace Predicate {
        function
/* default */ isEqual<T extends Object>(arg0: Object): Predicate<T>;
        function
/* default */ not<T extends Object>(arg0: Predicate<T>): Predicate<T>;
    }

    export interface Predicate<T extends Object> extends Object {

        test(arg0: T): boolean;

/* default */ and(arg0: Predicate<T>): Predicate<T>;

/* default */ negate(): Predicate<T>;

/* default */ or(arg0: Predicate<T>): Predicate<T>;
    }

    export interface Supplier<T extends Object> extends Object {

        get(): T;
    }

    export interface ToDoubleBiFunction<T extends Object, U extends Object> extends Object {

        applyAsDouble(arg0: T, arg1: U): number;
    }

    export interface ToDoubleFunction<T extends Object> extends Object {

        applyAsDouble(arg0: T): number;
    }

    export interface ToIntBiFunction<T extends Object, U extends Object> extends Object {

        applyAsInt(arg0: T, arg1: U): number;
    }

    export interface ToIntFunction<T extends Object> extends Object {

        applyAsInt(arg0: T): number;
    }

    export interface ToLongBiFunction<T extends Object, U extends Object> extends Object {

        applyAsLong(arg0: T, arg1: U): number;
    }

    export interface ToLongFunction<T extends Object> extends Object {

        applyAsLong(arg0: T): number;
    }

    export namespace UnaryOperator {
        function
/* default */ identity<T extends Object>(): UnaryOperator<T>;
    }

    export interface UnaryOperator<T extends Object> extends Function<T, T>, Object {
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.random.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.util.regex.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.math.d.ts" />
declare module '@java/java.util' {
    import { Permission, BasicPermission, PermissionCollection } from '@java/java.security'
    import { Stream, DoubleStream, LongStream, IntStream } from '@java/java.util.stream'
    import { RandomGenerator } from '@java/java.util.random'
    import { ByteBuffer, LongBuffer } from '@java/java.nio'
    import { ReadableByteChannel } from '@java/java.nio.channels'
    import { Path } from '@java/java.nio.file'
    import { Charset } from '@java/java.nio.charset'
    import { Enum, IllegalStateException, Comparable, Iterable, Character, Appendable, CharSequence, Error, String, Double, Exception, Integer, RuntimeException, Runnable, Long, Throwable, ClassLoader, Cloneable, Class, Readable, ModuleLayer, Boolean, Module, IllegalArgumentException } from '@java/java.lang'
    import { ZoneId, ZonedDateTime, Instant } from '@java/java.time'
    import { Pattern, MatchResult } from '@java/java.util.regex'
    import { PrintStream, Serializable, InputStream, OutputStream, Closeable, Reader, IOException, Flushable, File, Writer, PrintWriter } from '@java/java.io'
    import { LongBinaryOperator, IntSupplier, IntUnaryOperator, IntToDoubleFunction, Predicate, IntConsumer, Function, ToLongFunction, LongConsumer, DoubleBinaryOperator, DoubleConsumer, Consumer, BiFunction, DoubleSupplier, IntFunction, IntBinaryOperator, IntToLongFunction, ToIntFunction, Supplier, UnaryOperator, BinaryOperator, LongSupplier, BiConsumer, ToDoubleFunction } from '@java/java.util.function'
    import { BigInteger, BigDecimal } from '@java/java.math'
    export interface AbstractCollection<E extends Object> extends Collection<E> { }
    export abstract class AbstractCollection<E extends Object> extends Object implements Collection<E> {

        abstract iterator(): Iterator<E>;

        abstract size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        clear(): void;
        toString(): string;
    }

    export interface AbstractList<E extends Object> extends List<E> { }
    export abstract class AbstractList<E extends Object> extends AbstractCollection<E> implements List<E> {

        add(arg0: E): boolean;

        abstract get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        indexOf(arg0: Object): number;

        lastIndexOf(arg0: Object): number;

        clear(): void;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        iterator(): Iterator<E>;

        listIterator(): ListIterator<E>;

        listIterator(arg0: number): ListIterator<E>;

        subList(arg0: number, arg1: number): List<E>;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export interface AbstractMap<K extends Object, V extends Object> extends Map<K, V> { }
    export abstract class AbstractMap<K extends Object, V extends Object> extends Object implements Map<K, V> {

        size(): number;

        isEmpty(): boolean;

        containsValue(arg0: Object): boolean;

        containsKey(arg0: Object): boolean;

        get(arg0: Object): V;

        put(arg0: K, arg1: V): V;

        remove(arg0: Object): V;

        putAll(arg0: Map<K, V>): void;

        clear(): void;

        keySet(): Set<K>;

        values(): Collection<V>;

        abstract entrySet(): Set<Map.Entry<K, V>>;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }
    export namespace AbstractMap {
        export class SimpleEntry<K extends Object, V extends Object> extends Object implements Map.Entry<K, V>, Serializable {
            constructor(arg0: K, arg1: V);
            constructor(arg0: Map.Entry<K, V>);

            getKey(): K;

            getValue(): V;

            setValue(arg0: V): V;

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

        export class SimpleImmutableEntry<K extends Object, V extends Object> extends Object implements Map.Entry<K, V>, Serializable {
            constructor(arg0: K, arg1: V);
            constructor(arg0: Map.Entry<K, V>);

            getKey(): K;

            getValue(): V;

            setValue(arg0: V): V;

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

    }

    export interface AbstractQueue<E extends Object> extends Queue<E> { }
    export abstract class AbstractQueue<E extends Object> extends AbstractCollection<E> implements Queue<E> {

        add(arg0: E): boolean;

        remove(): E;

        element(): E;

        clear(): void;

        addAll(arg0: Collection<E>): boolean;
    }

    export interface AbstractSequentialList<E extends Object> { }
    export abstract class AbstractSequentialList<E extends Object> extends AbstractList<E> {

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        iterator(): Iterator<E>;

        abstract listIterator(arg0: number): ListIterator<E>;
    }

    export interface AbstractSet<E extends Object> extends Set<E> { }
    export abstract class AbstractSet<E extends Object> extends AbstractCollection<E> implements Set<E> {

        equals(arg0: Object): boolean;

        hashCode(): number;

        removeAll(arg0: Collection<any>): boolean;
    }

    export interface ArrayDeque<E extends Object> extends Deque<E>, Cloneable, Serializable { }
    export class ArrayDeque<E extends Object> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Collection<E>);

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        addAll(arg0: Collection<E>): boolean;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        removeFirst(): E;

        removeLast(): E;

        pollFirst(): E;

        pollLast(): E;

        getFirst(): E;

        getLast(): E;

        peekFirst(): E;

        peekLast(): E;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        remove(): E;

        poll(): E;

        element(): E;

        peek(): E;

        push(arg0: E): void;

        pop(): E;

        size(): number;

        isEmpty(): boolean;

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        forEach(arg0: Consumer<E>): void;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        contains(arg0: Object): boolean;

        remove(arg0: Object): boolean;

        clear(): void;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        clone(): ArrayDeque<E>;
    }

    export interface ArrayList<E extends Object> extends List<E>, RandomAccess, Cloneable, Serializable { }
    export class ArrayList<E extends Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
        constructor(arg0: number);
        constructor();
        constructor(arg0: Collection<E>);

        trimToSize(): void;

        ensureCapacity(arg0: number): void;

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        indexOf(arg0: Object): number;

        lastIndexOf(arg0: Object): number;

        clone(): Object;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: E): boolean;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        equals(arg0: Object): boolean;

        hashCode(): number;

        remove(arg0: Object): boolean;

        clear(): void;

        addAll(arg0: Collection<E>): boolean;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        listIterator(arg0: number): ListIterator<E>;

        listIterator(): ListIterator<E>;

        iterator(): Iterator<E>;

        subList(arg0: number, arg1: number): List<E>;

        forEach(arg0: Consumer<E>): void;

        spliterator(): Spliterator<E>;

        removeIf(arg0: Predicate<E>): boolean;

        replaceAll(arg0: UnaryOperator<E>): void;

        sort(arg0: Comparator<E>): void;
    }

    export class Arrays {

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static sort(arg0: String[]): void;

        static sort(arg0: String[], arg1: number, arg2: number): void;

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static sort(arg0: number[]): void;

        static sort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: String[]): void;

        static parallelSort(arg0: String[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort(arg0: number[]): void;

        static parallelSort(arg0: number[], arg1: number, arg2: number): void;

        static parallelSort<T extends Comparable<T>>(arg0: T[]): void;

        static parallelSort<T extends Comparable<T>>(arg0: T[], arg1: number, arg2: number): void;

        static parallelSort<T extends Object>(arg0: T[], arg1: Comparator<T>): void;

        static parallelSort<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: Comparator<T>): void;

        static sort(arg0: Object[]): void;

        static sort(arg0: Object[], arg1: number, arg2: number): void;

        static sort<T extends Object>(arg0: T[], arg1: Comparator<T>): void;

        static sort<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: Comparator<T>): void;

        static parallelPrefix<T extends Object>(arg0: T[], arg1: BinaryOperator<T>): void;

        static parallelPrefix<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: BinaryOperator<T>): void;

        static parallelPrefix(arg0: number[], arg1: LongBinaryOperator): void;

        static parallelPrefix(arg0: number[], arg1: number, arg2: number, arg3: LongBinaryOperator): void;

        static parallelPrefix(arg0: number[], arg1: DoubleBinaryOperator): void;

        static parallelPrefix(arg0: number[], arg1: number, arg2: number, arg3: DoubleBinaryOperator): void;

        static parallelPrefix(arg0: number[], arg1: IntBinaryOperator): void;

        static parallelPrefix(arg0: number[], arg1: number, arg2: number, arg3: IntBinaryOperator): void;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: String[], arg1: String): number;

        static binarySearch(arg0: String[], arg1: number, arg2: number, arg3: String): number;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: number[], arg1: number): number;

        static binarySearch(arg0: number[], arg1: number, arg2: number, arg3: number): number;

        static binarySearch(arg0: Object[], arg1: Object): number;

        static binarySearch(arg0: Object[], arg1: number, arg2: number, arg3: Object): number;

        static binarySearch<T extends Object>(arg0: T[], arg1: T, arg2: Comparator<T>): number;

        static binarySearch<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: T, arg4: Comparator<T>): number;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: String[], arg1: String[]): boolean;

        static equals(arg0: String[], arg1: number, arg2: number, arg3: String[], arg4: number, arg5: number): boolean;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: boolean[], arg1: boolean[]): boolean;

        static equals(arg0: boolean[], arg1: number, arg2: number, arg3: boolean[], arg4: number, arg5: number): boolean;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: number[], arg1: number[]): boolean;

        static equals(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): boolean;

        static equals(arg0: Object[], arg1: Object[]): boolean;

        static equals(arg0: Object[], arg1: number, arg2: number, arg3: Object[], arg4: number, arg5: number): boolean;

        static equals<T extends Object>(arg0: T[], arg1: T[], arg2: Comparator<T>): boolean;

        static equals<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: T[], arg4: number, arg5: number, arg6: Comparator<T>): boolean;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: String[], arg1: String): void;

        static fill(arg0: String[], arg1: number, arg2: number, arg3: String): void;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: boolean[], arg1: boolean): void;

        static fill(arg0: boolean[], arg1: number, arg2: number, arg3: boolean): void;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: number[], arg1: number): void;

        static fill(arg0: number[], arg1: number, arg2: number, arg3: number): void;

        static fill(arg0: Object[], arg1: Object): void;

        static fill(arg0: Object[], arg1: number, arg2: number, arg3: Object): void;

        static copyOf<T extends Object>(arg0: T[], arg1: number): T[];

        static copyOf<T extends Object, U extends Object>(arg0: U[], arg1: number, arg2: Class<T[]>): T[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: String[], arg1: number): String[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: number[], arg1: number): number[];

        static copyOf(arg0: boolean[], arg1: number): boolean[];

        static copyOfRange<T extends Object>(arg0: T[], arg1: number, arg2: number): T[];

        static copyOfRange<T extends Object, U extends Object>(arg0: U[], arg1: number, arg2: number, arg3: Class<T[]>): T[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: String[], arg1: number, arg2: number): String[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: number[], arg1: number, arg2: number): number[];

        static copyOfRange(arg0: boolean[], arg1: number, arg2: number): boolean[];

        static asList<T extends Object>(arg0: T[]): List<T>;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: String[]): number;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: boolean[]): number;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: number[]): number;

        static hashCode(arg0: Object[]): number;

        static deepHashCode(arg0: Object[]): number;

        static deepEquals(arg0: Object[], arg1: Object[]): boolean;

        static toString(arg0: number[]): String;

        static toString(arg0: number[]): String;

        static toString(arg0: number[]): String;

        static toString(arg0: String[]): String;

        static toString(arg0: number[]): String;

        static toString(arg0: boolean[]): String;

        static toString(arg0: number[]): String;

        static toString(arg0: number[]): String;

        static toString(arg0: Object[]): String;

        static deepToString(arg0: Object[]): String;

        static setAll<T extends Object>(arg0: T[], arg1: IntFunction<T>): void;

        static parallelSetAll<T extends Object>(arg0: T[], arg1: IntFunction<T>): void;

        static setAll(arg0: number[], arg1: IntUnaryOperator): void;

        static parallelSetAll(arg0: number[], arg1: IntUnaryOperator): void;

        static setAll(arg0: number[], arg1: IntToLongFunction): void;

        static parallelSetAll(arg0: number[], arg1: IntToLongFunction): void;

        static setAll(arg0: number[], arg1: IntToDoubleFunction): void;

        static parallelSetAll(arg0: number[], arg1: IntToDoubleFunction): void;

        static spliterator<T extends Object>(arg0: T[]): Spliterator<T>;

        static spliterator<T extends Object>(arg0: T[], arg1: number, arg2: number): Spliterator<T>;

        static spliterator(arg0: number[]): Spliterator.OfInt;

        static spliterator(arg0: number[], arg1: number, arg2: number): Spliterator.OfInt;

        static spliterator(arg0: number[]): Spliterator.OfLong;

        static spliterator(arg0: number[], arg1: number, arg2: number): Spliterator.OfLong;

        static spliterator(arg0: number[]): Spliterator.OfDouble;

        static spliterator(arg0: number[], arg1: number, arg2: number): Spliterator.OfDouble;

        static stream<T extends Object>(arg0: T[]): Stream<T>;

        static stream<T extends Object>(arg0: T[], arg1: number, arg2: number): Stream<T>;

        static stream(arg0: number[]): IntStream;

        static stream(arg0: number[], arg1: number, arg2: number): IntStream;

        static stream(arg0: number[]): LongStream;

        static stream(arg0: number[], arg1: number, arg2: number): LongStream;

        static stream(arg0: number[]): DoubleStream;

        static stream(arg0: number[], arg1: number, arg2: number): DoubleStream;

        static compare(arg0: boolean[], arg1: boolean[]): number;

        static compare(arg0: boolean[], arg1: number, arg2: number, arg3: boolean[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compareUnsigned(arg0: number[], arg1: number[]): number;

        static compareUnsigned(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compareUnsigned(arg0: number[], arg1: number[]): number;

        static compareUnsigned(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare(arg0: String[], arg1: String[]): number;

        static compare(arg0: String[], arg1: number, arg2: number, arg3: String[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compareUnsigned(arg0: number[], arg1: number[]): number;

        static compareUnsigned(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compareUnsigned(arg0: number[], arg1: number[]): number;

        static compareUnsigned(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare(arg0: number[], arg1: number[]): number;

        static compare(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static compare<T extends Comparable<T>>(arg0: T[], arg1: T[]): number;

        static compare<T extends Comparable<T>>(arg0: T[], arg1: number, arg2: number, arg3: T[], arg4: number, arg5: number): number;

        static compare<T extends Object>(arg0: T[], arg1: T[], arg2: Comparator<T>): number;

        static compare<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: T[], arg4: number, arg5: number, arg6: Comparator<T>): number;

        static mismatch(arg0: boolean[], arg1: boolean[]): number;

        static mismatch(arg0: boolean[], arg1: number, arg2: number, arg3: boolean[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: String[], arg1: String[]): number;

        static mismatch(arg0: String[], arg1: number, arg2: number, arg3: String[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: number[], arg1: number[]): number;

        static mismatch(arg0: number[], arg1: number, arg2: number, arg3: number[], arg4: number, arg5: number): number;

        static mismatch(arg0: Object[], arg1: Object[]): number;

        static mismatch(arg0: Object[], arg1: number, arg2: number, arg3: Object[], arg4: number, arg5: number): number;

        static mismatch<T extends Object>(arg0: T[], arg1: T[], arg2: Comparator<T>): number;

        static mismatch<T extends Object>(arg0: T[], arg1: number, arg2: number, arg3: T[], arg4: number, arg5: number, arg6: Comparator<T>): number;
    }

    export class Base64 {

        static getEncoder(): Base64.Encoder;

        static getUrlEncoder(): Base64.Encoder;

        static getMimeEncoder(): Base64.Encoder;

        static getMimeEncoder(arg0: number, arg1: number[]): Base64.Encoder;

        static getDecoder(): Base64.Decoder;

        static getUrlDecoder(): Base64.Decoder;

        static getMimeDecoder(): Base64.Decoder;
    }
    export namespace Base64 {
        export class Decoder {

            decode(arg0: number[]): number[];

            decode(arg0: String): number[];

            decode(arg0: number[], arg1: number[]): number;

            decode(arg0: ByteBuffer): ByteBuffer;

            wrap(arg0: InputStream): InputStream;
        }

        export class Encoder {

            encode(arg0: number[]): number[];

            encode(arg0: number[], arg1: number[]): number;

            encodeToString(arg0: number[]): String;

            encode(arg0: ByteBuffer): ByteBuffer;

            wrap(arg0: OutputStream): OutputStream;

            withoutPadding(): Base64.Encoder;
        }

    }

    export class BitSet implements Cloneable, Serializable {
        constructor();
        constructor(arg0: number);

        static valueOf(arg0: number[]): BitSet;

        static valueOf(arg0: LongBuffer): BitSet;

        static valueOf(arg0: number[]): BitSet;

        static valueOf(arg0: ByteBuffer): BitSet;

        toByteArray(): number[];

        toLongArray(): number[];

        flip(arg0: number): void;

        flip(arg0: number, arg1: number): void;

        set(arg0: number): void;

        set(arg0: number, arg1: boolean): void;

        set(arg0: number, arg1: number): void;

        set(arg0: number, arg1: number, arg2: boolean): void;

        clear(arg0: number): void;

        clear(arg0: number, arg1: number): void;

        clear(): void;

        get(arg0: number): boolean;

        get(arg0: number, arg1: number): BitSet;

        nextSetBit(arg0: number): number;

        nextClearBit(arg0: number): number;

        previousSetBit(arg0: number): number;

        previousClearBit(arg0: number): number;

        length(): number;

        isEmpty(): boolean;

        intersects(arg0: BitSet): boolean;

        cardinality(): number;

        and(arg0: BitSet): void;

        or(arg0: BitSet): void;

        xor(arg0: BitSet): void;

        andNot(arg0: BitSet): void;

        hashCode(): number;

        size(): number;

        equals(arg0: Object): boolean;

        clone(): Object;
        toString(): string;

        stream(): IntStream;
    }

    export abstract class Calendar extends Object implements Serializable, Cloneable, Comparable<Calendar> {
        static ERA: number
        static YEAR: number
        static MONTH: number
        static WEEK_OF_YEAR: number
        static WEEK_OF_MONTH: number
        static DATE: number
        static DAY_OF_MONTH: number
        static DAY_OF_YEAR: number
        static DAY_OF_WEEK: number
        static DAY_OF_WEEK_IN_MONTH: number
        static AM_PM: number
        static HOUR: number
        static HOUR_OF_DAY: number
        static MINUTE: number
        static SECOND: number
        static MILLISECOND: number
        static ZONE_OFFSET: number
        static DST_OFFSET: number
        static FIELD_COUNT: number
        static SUNDAY: number
        static MONDAY: number
        static TUESDAY: number
        static WEDNESDAY: number
        static THURSDAY: number
        static FRIDAY: number
        static SATURDAY: number
        static JANUARY: number
        static FEBRUARY: number
        static MARCH: number
        static APRIL: number
        static MAY: number
        static JUNE: number
        static JULY: number
        static AUGUST: number
        static SEPTEMBER: number
        static OCTOBER: number
        static NOVEMBER: number
        static DECEMBER: number
        static UNDECIMBER: number
        static AM: number
        static PM: number
        static ALL_STYLES: number
        static SHORT: number
        static LONG: number
        static NARROW_FORMAT: number
        static NARROW_STANDALONE: number
        static SHORT_FORMAT: number
        static LONG_FORMAT: number
        static SHORT_STANDALONE: number
        static LONG_STANDALONE: number

        static getInstance(): Calendar;

        static getInstance(arg0: TimeZone): Calendar;

        static getInstance(arg0: Locale): Calendar;

        static getInstance(arg0: TimeZone, arg1: Locale): Calendar;

        static getAvailableLocales(): Locale[];

        getTime(): Date;

        setTime(arg0: Date): void;

        getTimeInMillis(): number;

        setTimeInMillis(arg0: number): void;

        get(arg0: number): number;

        set(arg0: number, arg1: number): void;

        set(arg0: number, arg1: number, arg2: number): void;

        set(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number): void;

        set(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number): void;

        clear(): void;

        clear(arg0: number): void;

        isSet(arg0: number): boolean;

        getDisplayName(arg0: number, arg1: number, arg2: Locale): String;

        getDisplayNames(arg0: number, arg1: number, arg2: Locale): Map<String, Number>;

        static getAvailableCalendarTypes(): Set<String>;

        getCalendarType(): String;

        equals(arg0: Object): boolean;

        hashCode(): number;

        before(arg0: Object): boolean;

        after(arg0: Object): boolean;

        compareTo(arg0: Calendar): number;

        abstract add(arg0: number, arg1: number): void;

        abstract roll(arg0: number, arg1: boolean): void;

        roll(arg0: number, arg1: number): void;

        setTimeZone(arg0: TimeZone): void;

        getTimeZone(): TimeZone;

        setLenient(arg0: boolean): void;

        isLenient(): boolean;

        setFirstDayOfWeek(arg0: number): void;

        getFirstDayOfWeek(): number;

        setMinimalDaysInFirstWeek(arg0: number): void;

        getMinimalDaysInFirstWeek(): number;

        isWeekDateSupported(): boolean;

        getWeekYear(): number;

        setWeekDate(arg0: number, arg1: number, arg2: number): void;

        getWeeksInWeekYear(): number;

        abstract getMinimum(arg0: number): number;

        abstract getMaximum(arg0: number): number;

        abstract getGreatestMinimum(arg0: number): number;

        abstract getLeastMaximum(arg0: number): number;

        getActualMinimum(arg0: number): number;

        getActualMaximum(arg0: number): number;

        clone(): Object;
        toString(): string;

        toInstant(): Instant;
    }
    export namespace Calendar {
        export class Builder {
            constructor();

            setInstant(arg0: number): Calendar.Builder;

            setInstant(arg0: Date): Calendar.Builder;

            set(arg0: number, arg1: number): Calendar.Builder;

            setFields(arg0: number[]): Calendar.Builder;

            setDate(arg0: number, arg1: number, arg2: number): Calendar.Builder;

            setTimeOfDay(arg0: number, arg1: number, arg2: number): Calendar.Builder;

            setTimeOfDay(arg0: number, arg1: number, arg2: number, arg3: number): Calendar.Builder;

            setWeekDate(arg0: number, arg1: number, arg2: number): Calendar.Builder;

            setTimeZone(arg0: TimeZone): Calendar.Builder;

            setLenient(arg0: boolean): Calendar.Builder;

            setCalendarType(arg0: String): Calendar.Builder;

            setLocale(arg0: Locale): Calendar.Builder;

            setWeekDefinition(arg0: number, arg1: number): Calendar.Builder;

            build(): Calendar;
        }

    }

    export interface Collection<E extends Object> extends Iterable<E>, Object {

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        iterator(): Iterator<E>;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

/* default */ toArray<T extends Object>(arg0: IntFunction<T[]>): T[];

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

/* default */ removeIf(arg0: Predicate<E>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        clear(): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

/* default */ spliterator(): Spliterator<E>;

/* default */ stream(): Stream<E>;

/* default */ parallelStream(): Stream<E>;
    }

    export class Collections {
        static EMPTY_SET: Set
        static EMPTY_LIST: List
        static EMPTY_MAP: Map

        static sort<T extends Comparable<T>>(arg0: List<T>): void;

        static sort<T extends Object>(arg0: List<T>, arg1: Comparator<T>): void;

        static binarySearch<T extends Object>(arg0: List<Comparable<T>>, arg1: T): number;

        static binarySearch<T extends Object>(arg0: List<T>, arg1: T, arg2: Comparator<T>): number;

        static reverse(arg0: List<any>): void;

        static shuffle(arg0: List<any>): void;

        static shuffle(arg0: List<any>, arg1: Random): void;

        static swap(arg0: List<any>, arg1: number, arg2: number): void;

        static fill<T extends Object>(arg0: List<T>, arg1: T): void;

        static copy<T extends Object>(arg0: List<T>, arg1: List<T>): void;

        static min<T extends Comparable<T> & Object>(arg0: Collection<T>): T;

        static min<T extends Object>(arg0: Collection<T>, arg1: Comparator<T>): T;

        static max<T extends Comparable<T> & Object>(arg0: Collection<T>): T;

        static max<T extends Object>(arg0: Collection<T>, arg1: Comparator<T>): T;

        static rotate(arg0: List<any>, arg1: number): void;

        static replaceAll<T extends Object>(arg0: List<T>, arg1: T, arg2: T): boolean;

        static indexOfSubList(arg0: List<any>, arg1: List<any>): number;

        static lastIndexOfSubList(arg0: List<any>, arg1: List<any>): number;

        static unmodifiableCollection<T extends Object>(arg0: Collection<T>): Collection<T>;

        static unmodifiableSet<T extends Object>(arg0: Set<T>): Set<T>;

        static unmodifiableSortedSet<T extends Object>(arg0: SortedSet<T>): SortedSet<T>;

        static unmodifiableNavigableSet<T extends Object>(arg0: NavigableSet<T>): NavigableSet<T>;

        static unmodifiableList<T extends Object>(arg0: List<T>): List<T>;

        static unmodifiableMap<K extends Object, V extends Object>(arg0: Map<K, V>): Map<K, V>;

        static unmodifiableSortedMap<K extends Object, V extends Object>(arg0: SortedMap<K, V>): SortedMap<K, V>;

        static unmodifiableNavigableMap<K extends Object, V extends Object>(arg0: NavigableMap<K, V>): NavigableMap<K, V>;

        static synchronizedCollection<T extends Object>(arg0: Collection<T>): Collection<T>;

        static synchronizedSet<T extends Object>(arg0: Set<T>): Set<T>;

        static synchronizedSortedSet<T extends Object>(arg0: SortedSet<T>): SortedSet<T>;

        static synchronizedNavigableSet<T extends Object>(arg0: NavigableSet<T>): NavigableSet<T>;

        static synchronizedList<T extends Object>(arg0: List<T>): List<T>;

        static synchronizedMap<K extends Object, V extends Object>(arg0: Map<K, V>): Map<K, V>;

        static synchronizedSortedMap<K extends Object, V extends Object>(arg0: SortedMap<K, V>): SortedMap<K, V>;

        static synchronizedNavigableMap<K extends Object, V extends Object>(arg0: NavigableMap<K, V>): NavigableMap<K, V>;

        static checkedCollection<E extends Object>(arg0: Collection<E>, arg1: Class<E>): Collection<E>;

        static checkedQueue<E extends Object>(arg0: Queue<E>, arg1: Class<E>): Queue<E>;

        static checkedSet<E extends Object>(arg0: Set<E>, arg1: Class<E>): Set<E>;

        static checkedSortedSet<E extends Object>(arg0: SortedSet<E>, arg1: Class<E>): SortedSet<E>;

        static checkedNavigableSet<E extends Object>(arg0: NavigableSet<E>, arg1: Class<E>): NavigableSet<E>;

        static checkedList<E extends Object>(arg0: List<E>, arg1: Class<E>): List<E>;

        static checkedMap<K extends Object, V extends Object>(arg0: Map<K, V>, arg1: Class<K>, arg2: Class<V>): Map<K, V>;

        static checkedSortedMap<K extends Object, V extends Object>(arg0: SortedMap<K, V>, arg1: Class<K>, arg2: Class<V>): SortedMap<K, V>;

        static checkedNavigableMap<K extends Object, V extends Object>(arg0: NavigableMap<K, V>, arg1: Class<K>, arg2: Class<V>): NavigableMap<K, V>;

        static emptyIterator<T extends Object>(): Iterator<T>;

        static emptyListIterator<T extends Object>(): ListIterator<T>;

        static emptyEnumeration<T extends Object>(): Enumeration<T>;

        static emptySet<T extends Object>(): Set<T>;

        static emptySortedSet<E extends Object>(): SortedSet<E>;

        static emptyNavigableSet<E extends Object>(): NavigableSet<E>;

        static emptyList<T extends Object>(): List<T>;

        static emptyMap<K extends Object, V extends Object>(): Map<K, V>;

        static emptySortedMap<K extends Object, V extends Object>(): SortedMap<K, V>;

        static emptyNavigableMap<K extends Object, V extends Object>(): NavigableMap<K, V>;

        static singleton<T extends Object>(arg0: T): Set<T>;

        static singletonList<T extends Object>(arg0: T): List<T>;

        static singletonMap<K extends Object, V extends Object>(arg0: K, arg1: V): Map<K, V>;

        static nCopies<T extends Object>(arg0: number, arg1: T): List<T>;

        static reverseOrder<T extends Object>(): Comparator<T>;

        static reverseOrder<T extends Object>(arg0: Comparator<T>): Comparator<T>;

        static enumeration<T extends Object>(arg0: Collection<T>): Enumeration<T>;

        static list<T extends Object>(arg0: Enumeration<T>): ArrayList<T>;

        static frequency(arg0: Collection<any>, arg1: Object): number;

        static disjoint(arg0: Collection<any>, arg1: Collection<any>): boolean;

        static addAll<T extends Object>(arg0: Collection<T>, arg1: T[]): boolean;

        static newSetFromMap<E extends Object>(arg0: Map<E, Boolean>): Set<E>;

        static asLifoQueue<T extends Object>(arg0: Deque<T>): Queue<T>;
    }

    export namespace Comparator {
        function
/* default */ reverseOrder<T extends Comparable<T>>(): Comparator<T>;
        function
/* default */ naturalOrder<T extends Comparable<T>>(): Comparator<T>;
        function
/* default */ nullsFirst<T extends Object>(arg0: Comparator<T>): Comparator<T>;
        function
/* default */ nullsLast<T extends Object>(arg0: Comparator<T>): Comparator<T>;
        function
/* default */ comparing<T extends Object, U extends Object>(arg0: Function<T, U>, arg1: Comparator<U>): Comparator<T>;
        function
/* default */ comparing<T extends Object, U extends Comparable<U>>(arg0: Function<T, U>): Comparator<T>;
        function
/* default */ comparingInt<T extends Object>(arg0: ToIntFunction<T>): Comparator<T>;
        function
/* default */ comparingLong<T extends Object>(arg0: ToLongFunction<T>): Comparator<T>;
        function
/* default */ comparingDouble<T extends Object>(arg0: ToDoubleFunction<T>): Comparator<T>;
    }

    export interface Comparator<T extends Object> extends Object {

        compare(arg0: T, arg1: T): number;

        equals(arg0: Object): boolean;

/* default */ reversed(): Comparator<T>;

/* default */ thenComparing(arg0: Comparator<T>): Comparator<T>;

/* default */ thenComparing<U extends Object>(arg0: Function<T, U>, arg1: Comparator<U>): Comparator<T>;

/* default */ thenComparing<U extends Comparable<U>>(arg0: Function<T, U>): Comparator<T>;

/* default */ thenComparingInt(arg0: ToIntFunction<T>): Comparator<T>;

/* default */ thenComparingLong(arg0: ToLongFunction<T>): Comparator<T>;

/* default */ thenComparingDouble(arg0: ToDoubleFunction<T>): Comparator<T>;
    }

    export class ConcurrentModificationException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export class Currency implements Serializable {

        static getInstance(arg0: String): Currency;

        static getInstance(arg0: Locale): Currency;

        static getAvailableCurrencies(): Set<Currency>;

        getCurrencyCode(): String;

        getSymbol(): String;

        getSymbol(arg0: Locale): String;

        getDefaultFractionDigits(): number;

        getNumericCode(): number;

        getNumericCodeAsString(): String;

        getDisplayName(): String;

        getDisplayName(arg0: Locale): String;
        toString(): string;
    }

    export class Date extends Object implements Serializable, Cloneable, Comparable<Date> {
        constructor();
        constructor(arg0: number);
        constructor(arg0: number, arg1: number, arg2: number);
        constructor(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number);
        constructor(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number);
        constructor(arg0: String);

        clone(): Object;

        static UTC(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number): number;

        static parse(arg0: String): number;

        getYear(): number;

        setYear(arg0: number): void;

        getMonth(): number;

        setMonth(arg0: number): void;

        getDate(): number;

        setDate(arg0: number): void;

        getDay(): number;

        getHours(): number;

        setHours(arg0: number): void;

        getMinutes(): number;

        setMinutes(arg0: number): void;

        getSeconds(): number;

        setSeconds(arg0: number): void;

        getTime(): number;

        setTime(arg0: number): void;

        before(arg0: Date): boolean;

        after(arg0: Date): boolean;

        equals(arg0: Object): boolean;

        compareTo(arg0: Date): number;

        hashCode(): number;
        toString(): string;

        toLocaleString(): String;

        toGMTString(): String;

        getTimezoneOffset(): number;

        static from(arg0: Instant): Date;

        toInstant(): Instant;
    }

    export interface Deque<E extends Object> extends Queue<E>, Object {

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        removeFirst(): E;

        removeLast(): E;

        pollFirst(): E;

        pollLast(): E;

        getFirst(): E;

        getLast(): E;

        peekFirst(): E;

        peekLast(): E;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        remove(): E;

        poll(): E;

        element(): E;

        peek(): E;

        addAll(arg0: Collection<E>): boolean;

        push(arg0: E): void;

        pop(): E;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        size(): number;

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;
    }

    export abstract class Dictionary<K extends Object, V extends Object> extends Object {
        constructor();

        abstract size(): number;

        abstract isEmpty(): boolean;

        abstract keys(): Enumeration<K>;

        abstract elements(): Enumeration<V>;

        abstract get(arg0: Object): V;

        abstract put(arg0: K, arg1: V): V;

        abstract remove(arg0: Object): V;
    }

    export interface DoubleSummaryStatistics extends DoubleConsumer { }
    export class DoubleSummaryStatistics implements DoubleConsumer {
        constructor();
        constructor(arg0: number, arg1: number, arg2: number, arg3: number);

        accept(arg0: number): void;

        combine(arg0: DoubleSummaryStatistics): void;

        getCount(): number;

        getSum(): number;

        getMin(): number;

        getMax(): number;

        getAverage(): number;
        toString(): string;
    }

    export class DuplicateFormatFlagsException extends IllegalFormatException {
        constructor(arg0: String);

        getFlags(): String;

        getMessage(): String;
    }

    export class EmptyStackException extends RuntimeException {
        constructor();
    }

    export interface EnumMap<K extends Enum<K>, V extends Object> extends Serializable, Cloneable { }
    export class EnumMap<K extends Enum<K>, V extends Object> extends AbstractMap<K, V> implements Serializable, Cloneable {
        constructor(arg0: Class<K>);
        constructor(arg0: EnumMap<K, V>);
        constructor(arg0: Map<K, V>);

        size(): number;

        containsValue(arg0: Object): boolean;

        containsKey(arg0: Object): boolean;

        get(arg0: Object): V;

        put(arg0: K, arg1: V): V;

        remove(arg0: Object): V;

        putAll(arg0: Map<K, V>): void;

        clear(): void;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): EnumMap<K, V>;
    }

    export interface EnumSet<E extends Enum<E>> extends Cloneable, Serializable { }
    export abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable {

        static noneOf<E extends Enum<E>>(arg0: Class<E>): EnumSet<E>;

        static allOf<E extends Enum<E>>(arg0: Class<E>): EnumSet<E>;

        static copyOf<E extends Enum<E>>(arg0: EnumSet<E>): EnumSet<E>;

        static copyOf<E extends Enum<E>>(arg0: Collection<E>): EnumSet<E>;

        static complementOf<E extends Enum<E>>(arg0: EnumSet<E>): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E, arg1: E): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E, arg1: E, arg2: E): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E, arg1: E, arg2: E, arg3: E): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E): EnumSet<E>;

        static of<E extends Enum<E>>(arg0: E, arg1: E[]): EnumSet<E>;

        static range<E extends Enum<E>>(arg0: E, arg1: E): EnumSet<E>;

        clone(): EnumSet<E>;
    }

    export interface Enumeration<E extends Object> extends Object {

        hasMoreElements(): boolean;

        nextElement(): E;

/* default */ asIterator(): Iterator<E>;
    }

    export interface EventListener {
    }

    export abstract class EventListenerProxy<T extends EventListener> extends Object implements EventListener {
        constructor(arg0: T);

        getListener(): T;
    }

    export class EventObject implements Serializable {
        constructor(arg0: Object);

        getSource(): Object;
        toString(): string;
    }

    export class FormatFlagsConversionMismatchException extends IllegalFormatException {
        constructor(arg0: String, arg1: String);

        getFlags(): String;

        getConversion(): String;

        getMessage(): String;
    }

    export interface Formattable {

        formatTo(arg0: Formatter, arg1: number, arg2: number, arg3: number): void;
    }

    export class FormattableFlags {
        static LEFT_JUSTIFY: number
        static UPPERCASE: number
        static ALTERNATE: number
    }

    export class Formatter implements Closeable, Flushable {
        constructor();
        constructor(arg0: Appendable);
        constructor(arg0: Locale);
        constructor(arg0: Appendable, arg1: Locale);
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: Locale);
        constructor(arg0: String, arg1: Charset, arg2: Locale);
        constructor(arg0: File);
        constructor(arg0: File, arg1: String);
        constructor(arg0: File, arg1: String, arg2: Locale);
        constructor(arg0: File, arg1: Charset, arg2: Locale);
        constructor(arg0: PrintStream);
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: String);
        constructor(arg0: OutputStream, arg1: String, arg2: Locale);
        constructor(arg0: OutputStream, arg1: Charset, arg2: Locale);

        locale(): Locale;

        out(): Appendable;
        toString(): string;

        flush(): void;

        close(): void;

        ioException(): IOException;

        format(arg0: String, arg1: Object[]): Formatter;

        format(arg0: Locale, arg1: String, arg2: Object[]): Formatter;
    }
    export namespace Formatter {
        export class BigDecimalLayoutForm extends Enum<Formatter.BigDecimalLayoutForm> {
            static SCIENTIFIC: Formatter.BigDecimalLayoutForm
            static DECIMAL_FLOAT: Formatter.BigDecimalLayoutForm

            static values(): Formatter.BigDecimalLayoutForm[];

            static valueOf(arg0: String): Formatter.BigDecimalLayoutForm;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class FormatterClosedException extends IllegalStateException {
        constructor();
    }

    export class GregorianCalendar extends Calendar {
        static BC: number
        static AD: number
        constructor();
        constructor(arg0: TimeZone);
        constructor(arg0: Locale);
        constructor(arg0: TimeZone, arg1: Locale);
        constructor(arg0: number, arg1: number, arg2: number);
        constructor(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number);
        constructor(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number);

        setGregorianChange(arg0: Date): void;

        getGregorianChange(): Date;

        isLeapYear(arg0: number): boolean;

        getCalendarType(): String;

        equals(arg0: Object): boolean;

        hashCode(): number;

        add(arg0: number, arg1: number): void;

        roll(arg0: number, arg1: boolean): void;

        roll(arg0: number, arg1: number): void;

        getMinimum(arg0: number): number;

        getMaximum(arg0: number): number;

        getGreatestMinimum(arg0: number): number;

        getLeastMaximum(arg0: number): number;

        getActualMinimum(arg0: number): number;

        getActualMaximum(arg0: number): number;

        clone(): Object;

        getTimeZone(): TimeZone;

        setTimeZone(arg0: TimeZone): void;

        isWeekDateSupported(): boolean;

        getWeekYear(): number;

        setWeekDate(arg0: number, arg1: number, arg2: number): void;

        getWeeksInWeekYear(): number;

        toZonedDateTime(): ZonedDateTime;

        static from(arg0: ZonedDateTime): GregorianCalendar;
    }

    export class HashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Map<K, V>);

        size(): number;

        isEmpty(): boolean;

        get(arg0: Object): V;

        containsKey(arg0: Object): boolean;

        put(arg0: K, arg1: V): V;

        putAll(arg0: Map<K, V>): void;

        remove(arg0: Object): V;

        clear(): void;

        containsValue(arg0: Object): boolean;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        getOrDefault(arg0: Object, arg1: V): V;

        putIfAbsent(arg0: K, arg1: V): V;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

        computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

        computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

        compute(arg0: K, arg1: BiFunction<K, V, V>): V;

        merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;

        clone(): Object;
    }

    export interface HashSet<E extends Object> extends Set<E>, Cloneable, Serializable { }
    export class HashSet<E extends Object> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Collection<E>);
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);

        iterator(): Iterator<E>;

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        clear(): void;

        clone(): Object;

        spliterator(): Spliterator<E>;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];
    }

    export class Hashtable<K extends Object, V extends Object> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Map<K, V>);

        size(): number;

        isEmpty(): boolean;

        keys(): Enumeration<K>;

        elements(): Enumeration<V>;

        contains(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        containsKey(arg0: Object): boolean;

        get(arg0: Object): V;

        put(arg0: K, arg1: V): V;

        remove(arg0: Object): V;

        putAll(arg0: Map<K, V>): void;

        clear(): void;

        clone(): Object;
        toString(): string;

        keySet(): Set<K>;

        entrySet(): Set<Map.Entry<K, V>>;

        values(): Collection<V>;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getOrDefault(arg0: Object, arg1: V): V;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;

        putIfAbsent(arg0: K, arg1: V): V;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

        computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

        computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

        compute(arg0: K, arg1: BiFunction<K, V, V>): V;

        merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;
    }

    export class HexFormat {

        static of(): HexFormat;

        static ofDelimiter(arg0: String): HexFormat;

        withDelimiter(arg0: String): HexFormat;

        withPrefix(arg0: String): HexFormat;

        withSuffix(arg0: String): HexFormat;

        withUpperCase(): HexFormat;

        withLowerCase(): HexFormat;

        delimiter(): String;

        prefix(): String;

        suffix(): String;

        isUpperCase(): boolean;

        formatHex(arg0: number[]): String;

        formatHex(arg0: number[], arg1: number, arg2: number): String;

        formatHex<A extends Appendable>(arg0: A, arg1: number[]): A;

        formatHex<A extends Appendable>(arg0: A, arg1: number[], arg2: number, arg3: number): A;

        parseHex(arg0: CharSequence): number[];

        parseHex(arg0: CharSequence, arg1: number, arg2: number): number[];

        parseHex(arg0: String[], arg1: number, arg2: number): number[];

        toLowHexDigit(arg0: number): String;

        toHighHexDigit(arg0: number): String;

        toHexDigits<A extends Appendable>(arg0: A, arg1: number): A;

        toHexDigits(arg0: number): String;

        toHexDigits(arg0: String): String;

        toHexDigits(arg0: number): String;

        toHexDigits(arg0: number): String;

        toHexDigits(arg0: number): String;

        toHexDigits(arg0: number, arg1: number): String;

        static isHexDigit(arg0: number): boolean;

        static fromHexDigit(arg0: number): number;

        static fromHexDigits(arg0: CharSequence): number;

        static fromHexDigits(arg0: CharSequence, arg1: number, arg2: number): number;

        static fromHexDigitsToLong(arg0: CharSequence): number;

        static fromHexDigitsToLong(arg0: CharSequence, arg1: number, arg2: number): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface IdentityHashMap<K extends Object, V extends Object> extends Map<K, V>, Serializable, Cloneable { }
    export class IdentityHashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Map<K, V>);

        size(): number;

        isEmpty(): boolean;

        get(arg0: Object): V;

        containsKey(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        put(arg0: K, arg1: V): V;

        putAll(arg0: Map<K, V>): void;

        remove(arg0: Object): V;

        clear(): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): Object;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;
    }

    export class IllegalFormatCodePointException extends IllegalFormatException {
        constructor(arg0: number);

        getCodePoint(): number;

        getMessage(): String;
    }

    export class IllegalFormatConversionException extends IllegalFormatException {
        constructor(arg0: String, arg1: Class<any>);

        getConversion(): String;

        getArgumentClass(): Class<any>;

        getMessage(): String;
    }

    export class IllegalFormatException extends IllegalArgumentException {
    }

    export class IllegalFormatFlagsException extends IllegalFormatException {
        constructor(arg0: String);

        getFlags(): String;

        getMessage(): String;
    }

    export class IllegalFormatPrecisionException extends IllegalFormatException {
        constructor(arg0: number);

        getPrecision(): number;

        getMessage(): String;
    }

    export class IllegalFormatWidthException extends IllegalFormatException {
        constructor(arg0: number);

        getWidth(): number;

        getMessage(): String;
    }

    export class IllformedLocaleException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: number);

        getErrorIndex(): number;
    }

    export class InputMismatchException extends NoSuchElementException {
        constructor();
        constructor(arg0: String);
    }

    export interface IntSummaryStatistics extends IntConsumer { }
    export class IntSummaryStatistics implements IntConsumer {
        constructor();
        constructor(arg0: number, arg1: number, arg2: number, arg3: number);

        accept(arg0: number): void;

        combine(arg0: IntSummaryStatistics): void;

        getCount(): number;

        getSum(): number;

        getMin(): number;

        getMax(): number;

        getAverage(): number;
        toString(): string;
    }

    export class InvalidPropertiesFormatException extends IOException {
        constructor(arg0: Throwable);
        constructor(arg0: String);
    }

    export interface Iterator<E extends Object> extends Object {

        hasNext(): boolean;

        next(): E;

/* default */ remove(): void;

/* default */ forEachRemaining(arg0: Consumer<E>): void;
    }

    export interface LinkedHashMap<K extends Object, V extends Object> extends Map<K, V> { }
    export class LinkedHashMap<K extends Object, V extends Object> extends HashMap<K, V> implements Map<K, V> {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Map<K, V>);
        constructor(arg0: number, arg1: number, arg2: boolean);

        containsValue(arg0: Object): boolean;

        get(arg0: Object): V;

        getOrDefault(arg0: Object, arg1: V): V;

        clear(): void;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;
    }

    export interface LinkedHashSet<E extends Object> extends Set<E>, Cloneable, Serializable { }
    export class LinkedHashSet<E extends Object> extends HashSet<E> implements Set<E>, Cloneable, Serializable {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Collection<E>);

        spliterator(): Spliterator<E>;
    }

    export interface LinkedList<E extends Object> extends List<E>, Deque<E>, Cloneable, Serializable { }
    export class LinkedList<E extends Object> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Collection<E>);

        getFirst(): E;

        getLast(): E;

        removeFirst(): E;

        removeLast(): E;

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        contains(arg0: Object): boolean;

        size(): number;

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        addAll(arg0: Collection<E>): boolean;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        clear(): void;

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        indexOf(arg0: Object): number;

        lastIndexOf(arg0: Object): number;

        peek(): E;

        element(): E;

        poll(): E;

        remove(): E;

        offer(arg0: E): boolean;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        peekFirst(): E;

        peekLast(): E;

        pollFirst(): E;

        pollLast(): E;

        push(arg0: E): void;

        pop(): E;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        listIterator(arg0: number): ListIterator<E>;

        descendingIterator(): Iterator<E>;

        clone(): Object;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        spliterator(): Spliterator<E>;
    }

    export namespace List {
        function
/* default */ of<E extends Object>(): List<E>;
        function
/* default */ of<E extends Object>(arg0: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E, arg8: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E, arg8: E, arg9: E): List<E>;
        function
/* default */ of<E extends Object>(arg0: E[]): List<E>;
        function
/* default */ copyOf<E extends Object>(arg0: Collection<E>): List<E>;
    }

    export interface List<E extends Object> extends Collection<E>, Object {

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        iterator(): Iterator<E>;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

/* default */ replaceAll(arg0: UnaryOperator<E>): void;

/* default */ sort(arg0: Comparator<E>): void;

        clear(): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        indexOf(arg0: Object): number;

        lastIndexOf(arg0: Object): number;

        listIterator(): ListIterator<E>;

        listIterator(arg0: number): ListIterator<E>;

        subList(arg0: number, arg1: number): List<E>;

/* default */ spliterator(): Spliterator<E>;
    }

    export interface ListIterator<E extends Object> extends Iterator<E>, Object {

        hasNext(): boolean;

        next(): E;

        hasPrevious(): boolean;

        previous(): E;

        nextIndex(): number;

        previousIndex(): number;

        remove(): void;

        set(arg0: E): void;

        add(arg0: E): void;
    }

    export abstract class ListResourceBundle extends ResourceBundle {
        constructor();

        handleGetObject(arg0: String): Object;

        getKeys(): Enumeration<String>;
    }

    export class Locale implements Cloneable, Serializable {
        static ENGLISH: Locale
        static FRENCH: Locale
        static GERMAN: Locale
        static ITALIAN: Locale
        static JAPANESE: Locale
        static KOREAN: Locale
        static CHINESE: Locale
        static SIMPLIFIED_CHINESE: Locale
        static TRADITIONAL_CHINESE: Locale
        static FRANCE: Locale
        static GERMANY: Locale
        static ITALY: Locale
        static JAPAN: Locale
        static KOREA: Locale
        static UK: Locale
        static US: Locale
        static CANADA: Locale
        static CANADA_FRENCH: Locale
        static ROOT: Locale
        static CHINA: Locale
        static PRC: Locale
        static TAIWAN: Locale
        static PRIVATE_USE_EXTENSION: String
        static UNICODE_LOCALE_EXTENSION: String
        constructor(arg0: String, arg1: String, arg2: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String);

        static getDefault(): Locale;

        static getDefault(arg0: Locale.Category): Locale;

        static setDefault(arg0: Locale): void;

        static setDefault(arg0: Locale.Category, arg1: Locale): void;

        static getAvailableLocales(): Locale[];

        static getISOCountries(): String[];

        static getISOCountries(arg0: Locale.IsoCountryCode): Set<String>;

        static getISOLanguages(): String[];

        getLanguage(): String;

        getScript(): String;

        getCountry(): String;

        getVariant(): String;

        hasExtensions(): boolean;

        stripExtensions(): Locale;

        getExtension(arg0: String): String;

        getExtensionKeys(): Set<String>;

        getUnicodeLocaleAttributes(): Set<String>;

        getUnicodeLocaleType(arg0: String): String;

        getUnicodeLocaleKeys(): Set<String>;
        toString(): string;

        toLanguageTag(): String;

        static forLanguageTag(arg0: String): Locale;

        getISO3Language(): String;

        getISO3Country(): String;

        getDisplayLanguage(): String;

        getDisplayLanguage(arg0: Locale): String;

        getDisplayScript(): String;

        getDisplayScript(arg0: Locale): String;

        getDisplayCountry(): String;

        getDisplayCountry(arg0: Locale): String;

        getDisplayVariant(): String;

        getDisplayVariant(arg0: Locale): String;

        getDisplayName(): String;

        getDisplayName(arg0: Locale): String;

        clone(): Object;

        hashCode(): number;

        equals(arg0: Object): boolean;

        static filter(arg0: List<Locale.LanguageRange>, arg1: Collection<Locale>, arg2: Locale.FilteringMode): List<Locale>;

        static filter(arg0: List<Locale.LanguageRange>, arg1: Collection<Locale>): List<Locale>;

        static filterTags(arg0: List<Locale.LanguageRange>, arg1: Collection<String>, arg2: Locale.FilteringMode): List<String>;

        static filterTags(arg0: List<Locale.LanguageRange>, arg1: Collection<String>): List<String>;

        static lookup(arg0: List<Locale.LanguageRange>, arg1: Collection<Locale>): Locale;

        static lookupTag(arg0: List<Locale.LanguageRange>, arg1: Collection<String>): String;
    }
    export namespace Locale {
        export class Builder {
            constructor();

            setLocale(arg0: Locale): Locale.Builder;

            setLanguageTag(arg0: String): Locale.Builder;

            setLanguage(arg0: String): Locale.Builder;

            setScript(arg0: String): Locale.Builder;

            setRegion(arg0: String): Locale.Builder;

            setVariant(arg0: String): Locale.Builder;

            setExtension(arg0: String, arg1: String): Locale.Builder;

            setUnicodeLocaleKeyword(arg0: String, arg1: String): Locale.Builder;

            addUnicodeLocaleAttribute(arg0: String): Locale.Builder;

            removeUnicodeLocaleAttribute(arg0: String): Locale.Builder;

            clear(): Locale.Builder;

            clearExtensions(): Locale.Builder;

            build(): Locale;
        }

        export class Category extends Enum<Locale.Category> {
            static DISPLAY: Locale.Category
            static FORMAT: Locale.Category

            static values(): Locale.Category[];

            static valueOf(arg0: String): Locale.Category;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class FilteringMode extends Enum<Locale.FilteringMode> {
            static AUTOSELECT_FILTERING: Locale.FilteringMode
            static EXTENDED_FILTERING: Locale.FilteringMode
            static IGNORE_EXTENDED_RANGES: Locale.FilteringMode
            static MAP_EXTENDED_RANGES: Locale.FilteringMode
            static REJECT_EXTENDED_RANGES: Locale.FilteringMode

            static values(): Locale.FilteringMode[];

            static valueOf(arg0: String): Locale.FilteringMode;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export abstract class IsoCountryCode extends Enum<Locale.IsoCountryCode> {
            static PART1_ALPHA2: Locale.IsoCountryCode
            static PART1_ALPHA3: Locale.IsoCountryCode
            static PART3: Locale.IsoCountryCode

            static values(): Locale.IsoCountryCode[];

            static valueOf(arg0: String): Locale.IsoCountryCode;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class LanguageRange {
            static MAX_WEIGHT: number
            static MIN_WEIGHT: number
            constructor(arg0: String);
            constructor(arg0: String, arg1: number);

            getRange(): String;

            getWeight(): number;

            static parse(arg0: String): List<Locale.LanguageRange>;

            static parse(arg0: String, arg1: Map<String, List<String>>): List<Locale.LanguageRange>;

            static mapEquivalents(arg0: List<Locale.LanguageRange>, arg1: Map<String, List<String>>): List<Locale.LanguageRange>;

            hashCode(): number;

            equals(arg0: Object): boolean;
            toString(): string;
        }

    }

    export interface LongSummaryStatistics extends LongConsumer, IntConsumer { }
    export class LongSummaryStatistics implements LongConsumer, IntConsumer {
        constructor();
        constructor(arg0: number, arg1: number, arg2: number, arg3: number);

        accept(arg0: number): void;

        accept(arg0: number): void;

        combine(arg0: LongSummaryStatistics): void;

        getCount(): number;

        getSum(): number;

        getMin(): number;

        getMax(): number;

        getAverage(): number;
        toString(): string;
    }

    export namespace Map {
        function
/* default */ of<K extends Object, V extends Object>(): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V, arg10: K, arg11: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V, arg10: K, arg11: V, arg12: K, arg13: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V, arg10: K, arg11: V, arg12: K, arg13: V, arg14: K, arg15: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V, arg10: K, arg11: V, arg12: K, arg13: V, arg14: K, arg15: V, arg16: K, arg17: V): Map<K, V>;
        function
/* default */ of<K extends Object, V extends Object>(arg0: K, arg1: V, arg2: K, arg3: V, arg4: K, arg5: V, arg6: K, arg7: V, arg8: K, arg9: V, arg10: K, arg11: V, arg12: K, arg13: V, arg14: K, arg15: V, arg16: K, arg17: V, arg18: K, arg19: V): Map<K, V>;
        function
/* default */ ofEntries<K extends Object, V extends Object>(arg0: Map.Entry<K, V>[]): Map<K, V>;
        function
/* default */ entry<K extends Object, V extends Object>(arg0: K, arg1: V): Map.Entry<K, V>;
        function
/* default */ copyOf<K extends Object, V extends Object>(arg0: Map<K, V>): Map<K, V>;
    }

    export interface Map<K extends Object, V extends Object> extends Object {

        size(): number;

        isEmpty(): boolean;

        containsKey(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        get(arg0: Object): V;

        put(arg0: K, arg1: V): V;

        remove(arg0: Object): V;

        putAll(arg0: Map<K, V>): void;

        clear(): void;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        equals(arg0: Object): boolean;

        hashCode(): number;

/* default */ getOrDefault(arg0: Object, arg1: V): V;

/* default */ forEach(arg0: BiConsumer<K, V>): void;

/* default */ replaceAll(arg0: BiFunction<K, V, V>): void;

/* default */ putIfAbsent(arg0: K, arg1: V): V;

/* default */ remove(arg0: Object, arg1: Object): boolean;

/* default */ replace(arg0: K, arg1: V, arg2: V): boolean;

/* default */ replace(arg0: K, arg1: V): V;

/* default */ computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

/* default */ computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

/* default */ compute(arg0: K, arg1: BiFunction<K, V, V>): V;

/* default */ merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;
    }
    export namespace Map {
        export namespace Entry {
            function
/* default */ comparingByKey<K extends Comparable<K>, V extends Object>(): Comparator<Map.Entry<K, V>>;
            function
/* default */ comparingByValue<K extends Object, V extends Comparable<V>>(): Comparator<Map.Entry<K, V>>;
            function
/* default */ comparingByKey<K extends Object, V extends Object>(arg0: Comparator<K>): Comparator<Map.Entry<K, V>>;
            function
/* default */ comparingByValue<K extends Object, V extends Object>(arg0: Comparator<V>): Comparator<Map.Entry<K, V>>;
            function
/* default */ copyOf<K extends Object, V extends Object>(arg0: Map.Entry<K, V>): Map.Entry<K, V>;
        }

        export interface Entry<K extends Object, V extends Object> extends Object {

            getKey(): K;

            getValue(): V;

            setValue(arg0: V): V;

            equals(arg0: Object): boolean;

            hashCode(): number;
        }

    }

    export class MissingFormatArgumentException extends IllegalFormatException {
        constructor(arg0: String);

        getFormatSpecifier(): String;

        getMessage(): String;
    }

    export class MissingFormatWidthException extends IllegalFormatException {
        constructor(arg0: String);

        getFormatSpecifier(): String;

        getMessage(): String;
    }

    export class MissingResourceException extends RuntimeException {
        constructor(arg0: String, arg1: String, arg2: String);

        getClassName(): String;

        getKey(): String;
    }

    export interface NavigableMap<K extends Object, V extends Object> extends SortedMap<K, V>, Object {

        lowerEntry(arg0: K): Map.Entry<K, V>;

        lowerKey(arg0: K): K;

        floorEntry(arg0: K): Map.Entry<K, V>;

        floorKey(arg0: K): K;

        ceilingEntry(arg0: K): Map.Entry<K, V>;

        ceilingKey(arg0: K): K;

        higherEntry(arg0: K): Map.Entry<K, V>;

        higherKey(arg0: K): K;

        firstEntry(): Map.Entry<K, V>;

        lastEntry(): Map.Entry<K, V>;

        pollFirstEntry(): Map.Entry<K, V>;

        pollLastEntry(): Map.Entry<K, V>;

        descendingMap(): NavigableMap<K, V>;

        navigableKeySet(): NavigableSet<K>;

        descendingKeySet(): NavigableSet<K>;

        subMap(arg0: K, arg1: boolean, arg2: K, arg3: boolean): NavigableMap<K, V>;

        headMap(arg0: K, arg1: boolean): NavigableMap<K, V>;

        tailMap(arg0: K, arg1: boolean): NavigableMap<K, V>;

        subMap(arg0: K, arg1: K): SortedMap<K, V>;

        headMap(arg0: K): SortedMap<K, V>;

        tailMap(arg0: K): SortedMap<K, V>;
    }

    export interface NavigableSet<E extends Object> extends SortedSet<E>, Object {

        lower(arg0: E): E;

        floor(arg0: E): E;

        ceiling(arg0: E): E;

        higher(arg0: E): E;

        pollFirst(): E;

        pollLast(): E;

        iterator(): Iterator<E>;

        descendingSet(): NavigableSet<E>;

        descendingIterator(): Iterator<E>;

        subSet(arg0: E, arg1: boolean, arg2: E, arg3: boolean): NavigableSet<E>;

        headSet(arg0: E, arg1: boolean): NavigableSet<E>;

        tailSet(arg0: E, arg1: boolean): NavigableSet<E>;

        subSet(arg0: E, arg1: E): SortedSet<E>;

        headSet(arg0: E): SortedSet<E>;

        tailSet(arg0: E): SortedSet<E>;
    }

    export class NoSuchElementException extends RuntimeException {
        constructor();
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
        constructor(arg0: String);
    }

    export class Objects {

        static equals(arg0: Object, arg1: Object): boolean;

        static deepEquals(arg0: Object, arg1: Object): boolean;

        static hashCode(arg0: Object): number;

        static hash(arg0: Object[]): number;

        static toString(arg0: Object): String;

        static toString(arg0: Object, arg1: String): String;

        static compare<T extends Object>(arg0: T, arg1: T, arg2: Comparator<T>): number;

        static requireNonNull<T extends Object>(arg0: T): T;

        static requireNonNull<T extends Object>(arg0: T, arg1: String): T;

        static isNull(arg0: Object): boolean;

        static nonNull(arg0: Object): boolean;

        static requireNonNullElse<T extends Object>(arg0: T, arg1: T): T;

        static requireNonNullElseGet<T extends Object>(arg0: T, arg1: Supplier<T>): T;

        static requireNonNull<T extends Object>(arg0: T, arg1: Supplier<String>): T;

        static checkIndex(arg0: number, arg1: number): number;

        static checkFromToIndex(arg0: number, arg1: number, arg2: number): number;

        static checkFromIndexSize(arg0: number, arg1: number, arg2: number): number;

        static checkIndex(arg0: number, arg1: number): number;

        static checkFromToIndex(arg0: number, arg1: number, arg2: number): number;

        static checkFromIndexSize(arg0: number, arg1: number, arg2: number): number;
    }

    export class Observable {
        constructor();

        addObserver(arg0: Observer): void;

        deleteObserver(arg0: Observer): void;

        notifyObservers(): void;

        notifyObservers(arg0: Object): void;

        deleteObservers(): void;

        hasChanged(): boolean;

        countObservers(): number;
    }

    export interface Observer {

        update(arg0: Observable, arg1: Object): void;
    }

    export class Optional<T extends Object> extends Object {

        static empty<T extends Object>(): Optional<T>;

        static of<T extends Object>(arg0: T): Optional<T>;

        static ofNullable<T extends Object>(arg0: T): Optional<T>;

        get(): T;

        isPresent(): boolean;

        isEmpty(): boolean;

        ifPresent(arg0: Consumer<T>): void;

        ifPresentOrElse(arg0: Consumer<T>, arg1: Runnable): void;

        filter(arg0: Predicate<T>): Optional<T>;

        map<U extends Object>(arg0: Function<T, U>): Optional<U>;

        flatMap<U extends Object>(arg0: Function<T, Optional<U>>): Optional<U>;

        or(arg0: Supplier<Optional<T>>): Optional<T>;

        stream(): Stream<T>;

        orElse(arg0: T): T;

        orElseGet(arg0: Supplier<T>): T;

        orElseThrow(): T;

        orElseThrow<X extends Throwable>(arg0: Supplier<X>): T;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class OptionalDouble {

        static empty(): OptionalDouble;

        static of(arg0: number): OptionalDouble;

        getAsDouble(): number;

        isPresent(): boolean;

        isEmpty(): boolean;

        ifPresent(arg0: DoubleConsumer): void;

        ifPresentOrElse(arg0: DoubleConsumer, arg1: Runnable): void;

        stream(): DoubleStream;

        orElse(arg0: number): number;

        orElseGet(arg0: DoubleSupplier): number;

        orElseThrow(): number;

        orElseThrow<X extends Throwable>(arg0: Supplier<X>): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class OptionalInt {

        static empty(): OptionalInt;

        static of(arg0: number): OptionalInt;

        getAsInt(): number;

        isPresent(): boolean;

        isEmpty(): boolean;

        ifPresent(arg0: IntConsumer): void;

        ifPresentOrElse(arg0: IntConsumer, arg1: Runnable): void;

        stream(): IntStream;

        orElse(arg0: number): number;

        orElseGet(arg0: IntSupplier): number;

        orElseThrow(): number;

        orElseThrow<X extends Throwable>(arg0: Supplier<X>): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class OptionalLong {

        static empty(): OptionalLong;

        static of(arg0: number): OptionalLong;

        getAsLong(): number;

        isPresent(): boolean;

        isEmpty(): boolean;

        ifPresent(arg0: LongConsumer): void;

        ifPresentOrElse(arg0: LongConsumer, arg1: Runnable): void;

        stream(): LongStream;

        orElse(arg0: number): number;

        orElseGet(arg0: LongSupplier): number;

        orElseThrow(): number;

        orElseThrow<X extends Throwable>(arg0: Supplier<X>): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface PrimitiveIterator<T extends Object, T_CONS extends Object> extends Iterator<T>, Object {

        forEachRemaining(arg0: T_CONS): void;
    }
    export namespace PrimitiveIterator {
        export interface OfDouble extends PrimitiveIterator<Number, DoubleConsumer>, Object {

            nextDouble(): number;

/* default */ forEachRemaining(arg0: DoubleConsumer): void;

/* default */ next(): Number;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

        export interface OfInt extends PrimitiveIterator<Number, IntConsumer>, Object {

            nextInt(): number;

/* default */ forEachRemaining(arg0: IntConsumer): void;

/* default */ next(): Number;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

        export interface OfLong extends PrimitiveIterator<Number, LongConsumer>, Object {

            nextLong(): number;

/* default */ forEachRemaining(arg0: LongConsumer): void;

/* default */ next(): Number;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

    }

    export interface PriorityQueue<E extends Object> extends Serializable { }
    export class PriorityQueue<E extends Object> extends AbstractQueue<E> implements Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Comparator<E>);
        constructor(arg0: number, arg1: Comparator<E>);
        constructor(arg0: Collection<E>);
        constructor(arg0: PriorityQueue<E>);
        constructor(arg0: SortedSet<E>);

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        peek(): E;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        iterator(): Iterator<E>;

        size(): number;

        clear(): void;

        poll(): E;

        comparator(): Comparator<E>;

        spliterator(): Spliterator<E>;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        forEach(arg0: Consumer<E>): void;
    }

    export class Properties extends Hashtable<Object, Object> {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Properties);

        setProperty(arg0: String, arg1: String): Object;

        load(arg0: Reader): void;

        load(arg0: InputStream): void;

        save(arg0: OutputStream, arg1: String): void;

        store(arg0: Writer, arg1: String): void;

        store(arg0: OutputStream, arg1: String): void;

        loadFromXML(arg0: InputStream): void;

        storeToXML(arg0: OutputStream, arg1: String): void;

        storeToXML(arg0: OutputStream, arg1: String, arg2: String): void;

        storeToXML(arg0: OutputStream, arg1: String, arg2: Charset): void;

        getProperty(arg0: String): String;

        getProperty(arg0: String, arg1: String): String;

        propertyNames(): Enumeration<any>;

        stringPropertyNames(): Set<String>;

        list(arg0: PrintStream): void;

        list(arg0: PrintWriter): void;

        size(): number;

        isEmpty(): boolean;

        keys(): Enumeration<Object>;

        elements(): Enumeration<Object>;

        contains(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        containsKey(arg0: Object): boolean;

        get(arg0: Object): Object;

        put(arg0: Object, arg1: Object): Object;

        remove(arg0: Object): Object;

        putAll(arg0: Map<any, any>): void;

        clear(): void;
        toString(): string;

        keySet(): Set<Object>;

        values(): Collection<Object>;

        entrySet(): Set<Map.Entry<Object, Object>>;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getOrDefault(arg0: Object, arg1: Object): Object;

        forEach(arg0: BiConsumer<Object, Object>): void;

        replaceAll(arg0: BiFunction<Object, Object, any>): void;

        putIfAbsent(arg0: Object, arg1: Object): Object;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: Object, arg1: Object, arg2: Object): boolean;

        replace(arg0: Object, arg1: Object): Object;

        computeIfAbsent(arg0: Object, arg1: Function<Object, any>): Object;

        computeIfPresent(arg0: Object, arg1: BiFunction<Object, Object, any>): Object;

        compute(arg0: Object, arg1: BiFunction<Object, Object, any>): Object;

        merge(arg0: Object, arg1: Object, arg2: BiFunction<Object, Object, any>): Object;

        clone(): Object;
    }

    export class PropertyPermission extends BasicPermission {
        constructor(arg0: String, arg1: String);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        newPermissionCollection(): PermissionCollection;
    }

    export class PropertyResourceBundle extends ResourceBundle {
        constructor(arg0: InputStream);
        constructor(arg0: Reader);

        handleGetObject(arg0: String): Object;

        getKeys(): Enumeration<String>;
    }

    export interface Queue<E extends Object> extends Collection<E>, Object {

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        remove(): E;

        poll(): E;

        element(): E;

        peek(): E;
    }

    export interface Random extends RandomGenerator, Serializable { }
    export class Random implements RandomGenerator, Serializable {
        constructor();
        constructor(arg0: number);

        setSeed(arg0: number): void;

        nextBytes(arg0: number[]): void;

        nextInt(): number;

        nextInt(arg0: number): number;

        nextLong(): number;

        nextBoolean(): boolean;

        nextFloat(): number;

        nextDouble(): number;

        nextGaussian(): number;

        ints(arg0: number): IntStream;

        ints(): IntStream;

        ints(arg0: number, arg1: number, arg2: number): IntStream;

        ints(arg0: number, arg1: number): IntStream;

        longs(arg0: number): LongStream;

        longs(): LongStream;

        longs(arg0: number, arg1: number, arg2: number): LongStream;

        longs(arg0: number, arg1: number): LongStream;

        doubles(arg0: number): DoubleStream;

        doubles(): DoubleStream;

        doubles(arg0: number, arg1: number, arg2: number): DoubleStream;

        doubles(arg0: number, arg1: number): DoubleStream;
    }

    export interface RandomAccess {
    }

    export abstract class ResourceBundle {
        constructor();

        getBaseBundleName(): String;

        getString(arg0: String): String;

        getStringArray(arg0: String): String[];

        getObject(arg0: String): Object;

        getLocale(): Locale;

        static getBundle(arg0: String): ResourceBundle;

        static getBundle(arg0: String, arg1: ResourceBundle.Control): ResourceBundle;

        static getBundle(arg0: String, arg1: Locale): ResourceBundle;

        static getBundle(arg0: String, arg1: Module): ResourceBundle;

        static getBundle(arg0: String, arg1: Locale, arg2: Module): ResourceBundle;

        static getBundle(arg0: String, arg1: Locale, arg2: ResourceBundle.Control): ResourceBundle;

        static getBundle(arg0: String, arg1: Locale, arg2: ClassLoader): ResourceBundle;

        static getBundle(arg0: String, arg1: Locale, arg2: ClassLoader, arg3: ResourceBundle.Control): ResourceBundle;

        static clearCache(): void;

        static clearCache(arg0: ClassLoader): void;

        abstract getKeys(): Enumeration<String>;

        containsKey(arg0: String): boolean;

        keySet(): Set<String>;
    }
    export namespace ResourceBundle {
        export class Control {
            static FORMAT_DEFAULT: List<String>
            static FORMAT_CLASS: List<String>
            static FORMAT_PROPERTIES: List<String>
            static TTL_DONT_CACHE: number
            static TTL_NO_EXPIRATION_CONTROL: number

            static getControl(arg0: List<String>): ResourceBundle.Control;

            static getNoFallbackControl(arg0: List<String>): ResourceBundle.Control;

            getFormats(arg0: String): List<String>;

            getCandidateLocales(arg0: String, arg1: Locale): List<Locale>;

            getFallbackLocale(arg0: String, arg1: Locale): Locale;

            newBundle(arg0: String, arg1: Locale, arg2: String, arg3: ClassLoader, arg4: boolean): ResourceBundle;

            getTimeToLive(arg0: String, arg1: Locale): number;

            needsReload(arg0: String, arg1: Locale, arg2: String, arg3: ClassLoader, arg4: ResourceBundle, arg5: number): boolean;

            toBundleName(arg0: String, arg1: Locale): String;

            toResourceName(arg0: String, arg1: String): String;
        }

    }

    export interface Scanner extends Iterator<String>, Closeable { }
    export class Scanner extends Object implements Iterator<String>, Closeable {
        constructor(arg0: Readable);
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: String);
        constructor(arg0: InputStream, arg1: Charset);
        constructor(arg0: File);
        constructor(arg0: File, arg1: String);
        constructor(arg0: File, arg1: Charset);
        constructor(arg0: Path);
        constructor(arg0: Path, arg1: String);
        constructor(arg0: Path, arg1: Charset);
        constructor(arg0: String);
        constructor(arg0: ReadableByteChannel);
        constructor(arg0: ReadableByteChannel, arg1: String);
        constructor(arg0: ReadableByteChannel, arg1: Charset);

        close(): void;

        ioException(): IOException;

        delimiter(): Pattern;

        useDelimiter(arg0: Pattern): Scanner;

        useDelimiter(arg0: String): Scanner;

        locale(): Locale;

        useLocale(arg0: Locale): Scanner;

        radix(): number;

        useRadix(arg0: number): Scanner;

        match(): MatchResult;
        toString(): string;

        hasNext(): boolean;

        next(): String;

        remove(): void;

        hasNext(arg0: String): boolean;

        next(arg0: String): String;

        hasNext(arg0: Pattern): boolean;

        next(arg0: Pattern): String;

        hasNextLine(): boolean;

        nextLine(): String;

        findInLine(arg0: String): String;

        findInLine(arg0: Pattern): String;

        findWithinHorizon(arg0: String, arg1: number): String;

        findWithinHorizon(arg0: Pattern, arg1: number): String;

        skip(arg0: Pattern): Scanner;

        skip(arg0: String): Scanner;

        hasNextBoolean(): boolean;

        nextBoolean(): boolean;

        hasNextByte(): boolean;

        hasNextByte(arg0: number): boolean;

        nextByte(): number;

        nextByte(arg0: number): number;

        hasNextShort(): boolean;

        hasNextShort(arg0: number): boolean;

        nextShort(): number;

        nextShort(arg0: number): number;

        hasNextInt(): boolean;

        hasNextInt(arg0: number): boolean;

        nextInt(): number;

        nextInt(arg0: number): number;

        hasNextLong(): boolean;

        hasNextLong(arg0: number): boolean;

        nextLong(): number;

        nextLong(arg0: number): number;

        hasNextFloat(): boolean;

        nextFloat(): number;

        hasNextDouble(): boolean;

        nextDouble(): number;

        hasNextBigInteger(): boolean;

        hasNextBigInteger(arg0: number): boolean;

        nextBigInteger(): BigInteger;

        nextBigInteger(arg0: number): BigInteger;

        hasNextBigDecimal(): boolean;

        nextBigDecimal(): BigDecimal;

        reset(): Scanner;

        tokens(): Stream<String>;

        findAll(arg0: Pattern): Stream<MatchResult>;

        findAll(arg0: String): Stream<MatchResult>;
    }

    export class ServiceConfigurationError extends Error {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export interface ServiceLoader<S extends Object> extends Iterable<S> { }
    export class ServiceLoader<S extends Object> extends Object implements Iterable<S> {

        iterator(): Iterator<S>;

        stream(): Stream<ServiceLoader.Provider<S>>;

        static load<S extends Object>(arg0: Class<S>, arg1: ClassLoader): ServiceLoader<S>;

        static load<S extends Object>(arg0: Class<S>): ServiceLoader<S>;

        static loadInstalled<S extends Object>(arg0: Class<S>): ServiceLoader<S>;

        static load<S extends Object>(arg0: ModuleLayer, arg1: Class<S>): ServiceLoader<S>;

        findFirst(): Optional<S>;

        reload(): void;
        toString(): string;
    }
    export namespace ServiceLoader {
        export interface Provider<S extends Object> extends Supplier<S>, Object {

            type(): Class<S>;

            get(): S;
        }

    }

    export namespace Set {
        function
/* default */ of<E extends Object>(): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E, arg8: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E, arg1: E, arg2: E, arg3: E, arg4: E, arg5: E, arg6: E, arg7: E, arg8: E, arg9: E): Set<E>;
        function
/* default */ of<E extends Object>(arg0: E[]): Set<E>;
        function
/* default */ copyOf<E extends Object>(arg0: Collection<E>): Set<E>;
    }

    export interface Set<E extends Object> extends Collection<E>, Object {

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        iterator(): Iterator<E>;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        clear(): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

/* default */ spliterator(): Spliterator<E>;
    }

    export class SimpleTimeZone extends TimeZone {
        static WALL_TIME: number
        static STANDARD_TIME: number
        static UTC_TIME: number
        constructor(arg0: number, arg1: String);
        constructor(arg0: number, arg1: String, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: number, arg8: number, arg9: number);
        constructor(arg0: number, arg1: String, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: number, arg8: number, arg9: number, arg10: number);
        constructor(arg0: number, arg1: String, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: number, arg8: number, arg9: number, arg10: number, arg11: number, arg12: number);

        setStartYear(arg0: number): void;

        setStartRule(arg0: number, arg1: number, arg2: number, arg3: number): void;

        setStartRule(arg0: number, arg1: number, arg2: number): void;

        setStartRule(arg0: number, arg1: number, arg2: number, arg3: number, arg4: boolean): void;

        setEndRule(arg0: number, arg1: number, arg2: number, arg3: number): void;

        setEndRule(arg0: number, arg1: number, arg2: number): void;

        setEndRule(arg0: number, arg1: number, arg2: number, arg3: number, arg4: boolean): void;

        getOffset(arg0: number): number;

        getOffset(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number): number;

        getRawOffset(): number;

        setRawOffset(arg0: number): void;

        setDSTSavings(arg0: number): void;

        getDSTSavings(): number;

        useDaylightTime(): boolean;

        observesDaylightTime(): boolean;

        inDaylightTime(arg0: Date): boolean;

        clone(): Object;

        hashCode(): number;

        equals(arg0: Object): boolean;

        hasSameRules(arg0: TimeZone): boolean;
        toString(): string;
    }

    export interface SortedMap<K extends Object, V extends Object> extends Map<K, V>, Object {

        comparator(): Comparator<K>;

        subMap(arg0: K, arg1: K): SortedMap<K, V>;

        headMap(arg0: K): SortedMap<K, V>;

        tailMap(arg0: K): SortedMap<K, V>;

        firstKey(): K;

        lastKey(): K;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;
    }

    export interface SortedSet<E extends Object> extends Set<E>, Object {

        comparator(): Comparator<E>;

        subSet(arg0: E, arg1: E): SortedSet<E>;

        headSet(arg0: E): SortedSet<E>;

        tailSet(arg0: E): SortedSet<E>;

        first(): E;

        last(): E;

/* default */ spliterator(): Spliterator<E>;
    }

    export namespace Spliterator {
        const ORDERED: number
        const DISTINCT: number
        const SORTED: number
        const SIZED: number
        const NONNULL: number
        const IMMUTABLE: number
        const CONCURRENT: number
        const SUBSIZED: number
    }

    export interface Spliterator<T extends Object> extends Object {
        ORDERED: number
        DISTINCT: number
        SORTED: number
        SIZED: number
        NONNULL: number
        IMMUTABLE: number
        CONCURRENT: number
        SUBSIZED: number

        tryAdvance(arg0: Consumer<T>): boolean;

/* default */ forEachRemaining(arg0: Consumer<T>): void;

        trySplit(): Spliterator<T>;

        estimateSize(): number;

/* default */ getExactSizeIfKnown(): number;

        characteristics(): number;

/* default */ hasCharacteristics(arg0: number): boolean;

/* default */ getComparator(): Comparator<T>;
    }
    export namespace Spliterator {
        export interface OfDouble extends Spliterator.OfPrimitive<Number, DoubleConsumer, Spliterator.OfDouble>, Object {

            trySplit(): Spliterator.OfDouble;

            tryAdvance(arg0: DoubleConsumer): boolean;

/* default */ forEachRemaining(arg0: DoubleConsumer): void;

/* default */ tryAdvance(arg0: Consumer<Number>): boolean;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

        export interface OfInt extends Spliterator.OfPrimitive<Number, IntConsumer, Spliterator.OfInt>, Object {

            trySplit(): Spliterator.OfInt;

            tryAdvance(arg0: IntConsumer): boolean;

/* default */ forEachRemaining(arg0: IntConsumer): void;

/* default */ tryAdvance(arg0: Consumer<Number>): boolean;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

        export interface OfLong extends Spliterator.OfPrimitive<Number, LongConsumer, Spliterator.OfLong>, Object {

            trySplit(): Spliterator.OfLong;

            tryAdvance(arg0: LongConsumer): boolean;

/* default */ forEachRemaining(arg0: LongConsumer): void;

/* default */ tryAdvance(arg0: Consumer<Number>): boolean;

/* default */ forEachRemaining(arg0: Consumer<Number>): void;
        }

        export interface OfPrimitive<T extends Object, T_CONS extends Object, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends Spliterator<T>, Object {

            trySplit(): T_SPLITR;

            tryAdvance(arg0: T_CONS): boolean;

/* default */ forEachRemaining(arg0: T_CONS): void;
        }

    }

    export class Spliterators {

        static emptySpliterator<T extends Object>(): Spliterator<T>;

        static emptyIntSpliterator(): Spliterator.OfInt;

        static emptyLongSpliterator(): Spliterator.OfLong;

        static emptyDoubleSpliterator(): Spliterator.OfDouble;

        static spliterator<T extends Object>(arg0: Object[], arg1: number): Spliterator<T>;

        static spliterator<T extends Object>(arg0: Object[], arg1: number, arg2: number, arg3: number): Spliterator<T>;

        static spliterator(arg0: number[], arg1: number): Spliterator.OfInt;

        static spliterator(arg0: number[], arg1: number, arg2: number, arg3: number): Spliterator.OfInt;

        static spliterator(arg0: number[], arg1: number): Spliterator.OfLong;

        static spliterator(arg0: number[], arg1: number, arg2: number, arg3: number): Spliterator.OfLong;

        static spliterator(arg0: number[], arg1: number): Spliterator.OfDouble;

        static spliterator(arg0: number[], arg1: number, arg2: number, arg3: number): Spliterator.OfDouble;

        static spliterator<T extends Object>(arg0: Collection<T>, arg1: number): Spliterator<T>;

        static spliterator<T extends Object>(arg0: Iterator<T>, arg1: number, arg2: number): Spliterator<T>;

        static spliteratorUnknownSize<T extends Object>(arg0: Iterator<T>, arg1: number): Spliterator<T>;

        static spliterator(arg0: PrimitiveIterator.OfInt, arg1: number, arg2: number): Spliterator.OfInt;

        static spliteratorUnknownSize(arg0: PrimitiveIterator.OfInt, arg1: number): Spliterator.OfInt;

        static spliterator(arg0: PrimitiveIterator.OfLong, arg1: number, arg2: number): Spliterator.OfLong;

        static spliteratorUnknownSize(arg0: PrimitiveIterator.OfLong, arg1: number): Spliterator.OfLong;

        static spliterator(arg0: PrimitiveIterator.OfDouble, arg1: number, arg2: number): Spliterator.OfDouble;

        static spliteratorUnknownSize(arg0: PrimitiveIterator.OfDouble, arg1: number): Spliterator.OfDouble;

        static iterator<T extends Object>(arg0: Spliterator<T>): Iterator<T>;

        static iterator(arg0: Spliterator.OfInt): PrimitiveIterator.OfInt;

        static iterator(arg0: Spliterator.OfLong): PrimitiveIterator.OfLong;

        static iterator(arg0: Spliterator.OfDouble): PrimitiveIterator.OfDouble;
    }
    export namespace Spliterators {
        export interface AbstractDoubleSpliterator extends Spliterator.OfDouble { }
        export abstract class AbstractDoubleSpliterator implements Spliterator.OfDouble {

            trySplit(): Spliterator.OfDouble;

            estimateSize(): number;

            characteristics(): number;
        }

        export interface AbstractIntSpliterator extends Spliterator.OfInt { }
        export abstract class AbstractIntSpliterator implements Spliterator.OfInt {

            trySplit(): Spliterator.OfInt;

            estimateSize(): number;

            characteristics(): number;
        }

        export interface AbstractLongSpliterator extends Spliterator.OfLong { }
        export abstract class AbstractLongSpliterator implements Spliterator.OfLong {

            trySplit(): Spliterator.OfLong;

            estimateSize(): number;

            characteristics(): number;
        }

        export interface AbstractSpliterator<T extends Object> extends Spliterator<T> { }
        export abstract class AbstractSpliterator<T extends Object> extends Object implements Spliterator<T> {

            trySplit(): Spliterator<T>;

            estimateSize(): number;

            characteristics(): number;
        }

    }

    export interface SplittableRandom extends RandomGenerator, RandomGenerator.SplittableGenerator { }
    export class SplittableRandom implements RandomGenerator, RandomGenerator.SplittableGenerator {
        constructor(arg0: number);
        constructor();

        split(): SplittableRandom;

        split(arg0: RandomGenerator.SplittableGenerator): SplittableRandom;

        nextInt(): number;

        nextLong(): number;

        nextBytes(arg0: number[]): void;

        splits(): Stream<RandomGenerator.SplittableGenerator>;

        splits(arg0: number): Stream<RandomGenerator.SplittableGenerator>;

        splits(arg0: RandomGenerator.SplittableGenerator): Stream<RandomGenerator.SplittableGenerator>;

        splits(arg0: number, arg1: RandomGenerator.SplittableGenerator): Stream<RandomGenerator.SplittableGenerator>;

        ints(arg0: number): IntStream;

        ints(): IntStream;

        ints(arg0: number, arg1: number, arg2: number): IntStream;

        ints(arg0: number, arg1: number): IntStream;

        longs(arg0: number): LongStream;

        longs(): LongStream;

        longs(arg0: number, arg1: number, arg2: number): LongStream;

        longs(arg0: number, arg1: number): LongStream;

        doubles(arg0: number): DoubleStream;

        doubles(): DoubleStream;

        doubles(arg0: number, arg1: number, arg2: number): DoubleStream;

        doubles(arg0: number, arg1: number): DoubleStream;
    }

    export interface Stack<E extends Object> { }
    export class Stack<E extends Object> extends Vector<E> {
        constructor();

        push(arg0: E): E;

        pop(): E;

        peek(): E;

        empty(): boolean;

        search(arg0: Object): number;
    }

    export class StringJoiner {
        constructor(arg0: CharSequence);
        constructor(arg0: CharSequence, arg1: CharSequence, arg2: CharSequence);

        setEmptyValue(arg0: CharSequence): StringJoiner;
        toString(): string;

        add(arg0: CharSequence): StringJoiner;

        merge(arg0: StringJoiner): StringJoiner;

        length(): number;
    }

    export interface StringTokenizer extends Enumeration<Object> { }
    export class StringTokenizer extends Object implements Enumeration<Object> {
        constructor(arg0: String, arg1: String, arg2: boolean);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String);

        hasMoreTokens(): boolean;

        nextToken(): String;

        nextToken(arg0: String): String;

        hasMoreElements(): boolean;

        nextElement(): Object;

        countTokens(): number;
    }

    export abstract class TimeZone implements Serializable, Cloneable {
        static SHORT: number
        static LONG: number
        constructor();

        abstract getOffset(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number): number;

        getOffset(arg0: number): number;

        abstract setRawOffset(arg0: number): void;

        abstract getRawOffset(): number;

        getID(): String;

        setID(arg0: String): void;

        getDisplayName(): String;

        getDisplayName(arg0: Locale): String;

        getDisplayName(arg0: boolean, arg1: number): String;

        getDisplayName(arg0: boolean, arg1: number, arg2: Locale): String;

        getDSTSavings(): number;

        abstract useDaylightTime(): boolean;

        observesDaylightTime(): boolean;

        abstract inDaylightTime(arg0: Date): boolean;

        static getTimeZone(arg0: String): TimeZone;

        static getTimeZone(arg0: ZoneId): TimeZone;

        toZoneId(): ZoneId;

        static getAvailableIDs(arg0: number): String[];

        static getAvailableIDs(): String[];

        static getDefault(): TimeZone;

        static setDefault(arg0: TimeZone): void;

        hasSameRules(arg0: TimeZone): boolean;

        clone(): Object;
    }

    export class Timer {
        constructor();
        constructor(arg0: boolean);
        constructor(arg0: String);
        constructor(arg0: String, arg1: boolean);

        schedule(arg0: TimerTask, arg1: number): void;

        schedule(arg0: TimerTask, arg1: Date): void;

        schedule(arg0: TimerTask, arg1: number, arg2: number): void;

        schedule(arg0: TimerTask, arg1: Date, arg2: number): void;

        scheduleAtFixedRate(arg0: TimerTask, arg1: number, arg2: number): void;

        scheduleAtFixedRate(arg0: TimerTask, arg1: Date, arg2: number): void;

        cancel(): void;

        purge(): number;
    }

    export abstract class TimerTask implements Runnable {

        abstract run(): void;

        cancel(): boolean;

        scheduledExecutionTime(): number;
    }

    export class TooManyListenersException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface TreeMap<K extends Object, V extends Object> extends NavigableMap<K, V>, Cloneable, Serializable { }
    export class TreeMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements NavigableMap<K, V>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Comparator<K>);
        constructor(arg0: Map<K, V>);
        constructor(arg0: SortedMap<K, V>);

        size(): number;

        containsKey(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        get(arg0: Object): V;

        comparator(): Comparator<K>;

        firstKey(): K;

        lastKey(): K;

        putAll(arg0: Map<K, V>): void;

        put(arg0: K, arg1: V): V;

        putIfAbsent(arg0: K, arg1: V): V;

        computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

        computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

        compute(arg0: K, arg1: BiFunction<K, V, V>): V;

        merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;

        remove(arg0: Object): V;

        clear(): void;

        clone(): Object;

        firstEntry(): Map.Entry<K, V>;

        lastEntry(): Map.Entry<K, V>;

        pollFirstEntry(): Map.Entry<K, V>;

        pollLastEntry(): Map.Entry<K, V>;

        lowerEntry(arg0: K): Map.Entry<K, V>;

        lowerKey(arg0: K): K;

        floorEntry(arg0: K): Map.Entry<K, V>;

        floorKey(arg0: K): K;

        ceilingEntry(arg0: K): Map.Entry<K, V>;

        ceilingKey(arg0: K): K;

        higherEntry(arg0: K): Map.Entry<K, V>;

        higherKey(arg0: K): K;

        keySet(): Set<K>;

        navigableKeySet(): NavigableSet<K>;

        descendingKeySet(): NavigableSet<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        descendingMap(): NavigableMap<K, V>;

        subMap(arg0: K, arg1: boolean, arg2: K, arg3: boolean): NavigableMap<K, V>;

        headMap(arg0: K, arg1: boolean): NavigableMap<K, V>;

        tailMap(arg0: K, arg1: boolean): NavigableMap<K, V>;

        subMap(arg0: K, arg1: K): SortedMap<K, V>;

        headMap(arg0: K): SortedMap<K, V>;

        tailMap(arg0: K): SortedMap<K, V>;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;
    }

    export interface TreeSet<E extends Object> extends NavigableSet<E>, Cloneable, Serializable { }
    export class TreeSet<E extends Object> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Comparator<E>);
        constructor(arg0: Collection<E>);
        constructor(arg0: SortedSet<E>);

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;

        descendingSet(): NavigableSet<E>;

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        clear(): void;

        addAll(arg0: Collection<E>): boolean;

        subSet(arg0: E, arg1: boolean, arg2: E, arg3: boolean): NavigableSet<E>;

        headSet(arg0: E, arg1: boolean): NavigableSet<E>;

        tailSet(arg0: E, arg1: boolean): NavigableSet<E>;

        subSet(arg0: E, arg1: E): SortedSet<E>;

        headSet(arg0: E): SortedSet<E>;

        tailSet(arg0: E): SortedSet<E>;

        comparator(): Comparator<E>;

        first(): E;

        last(): E;

        lower(arg0: E): E;

        floor(arg0: E): E;

        ceiling(arg0: E): E;

        higher(arg0: E): E;

        pollFirst(): E;

        pollLast(): E;

        clone(): Object;

        spliterator(): Spliterator<E>;
    }

    export class UUID extends Object implements Serializable, Comparable<UUID> {
        constructor(arg0: number, arg1: number);

        static randomUUID(): UUID;

        static nameUUIDFromBytes(arg0: number[]): UUID;

        static fromString(arg0: String): UUID;

        getLeastSignificantBits(): number;

        getMostSignificantBits(): number;

        version(): number;

        variant(): number;

        timestamp(): number;

        clockSequence(): number;

        node(): number;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: UUID): number;
    }

    export class UnknownFormatConversionException extends IllegalFormatException {
        constructor(arg0: String);

        getConversion(): String;

        getMessage(): String;
    }

    export class UnknownFormatFlagsException extends IllegalFormatException {
        constructor(arg0: String);

        getFlags(): String;

        getMessage(): String;
    }

    export interface Vector<E extends Object> extends List<E>, RandomAccess, Cloneable, Serializable { }
    export class Vector<E extends Object> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Collection<E>);

        copyInto(arg0: Object[]): void;

        trimToSize(): void;

        ensureCapacity(arg0: number): void;

        setSize(arg0: number): void;

        capacity(): number;

        size(): number;

        isEmpty(): boolean;

        elements(): Enumeration<E>;

        contains(arg0: Object): boolean;

        indexOf(arg0: Object): number;

        indexOf(arg0: Object, arg1: number): number;

        lastIndexOf(arg0: Object): number;

        lastIndexOf(arg0: Object, arg1: number): number;

        elementAt(arg0: number): E;

        firstElement(): E;

        lastElement(): E;

        setElementAt(arg0: E, arg1: number): void;

        removeElementAt(arg0: number): void;

        insertElementAt(arg0: E, arg1: number): void;

        addElement(arg0: E): void;

        removeElement(arg0: Object): boolean;

        removeAllElements(): void;

        clone(): Object;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        clear(): void;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        removeIf(arg0: Predicate<E>): boolean;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        subList(arg0: number, arg1: number): List<E>;

        listIterator(arg0: number): ListIterator<E>;

        listIterator(): ListIterator<E>;

        iterator(): Iterator<E>;

        forEach(arg0: Consumer<E>): void;

        replaceAll(arg0: UnaryOperator<E>): void;

        sort(arg0: Comparator<E>): void;

        spliterator(): Spliterator<E>;
    }

    export interface WeakHashMap<K extends Object, V extends Object> extends Map<K, V> { }
    export class WeakHashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements Map<K, V> {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number);
        constructor();
        constructor(arg0: Map<K, V>);

        size(): number;

        isEmpty(): boolean;

        get(arg0: Object): V;

        containsKey(arg0: Object): boolean;

        put(arg0: K, arg1: V): V;

        putAll(arg0: Map<K, V>): void;

        remove(arg0: Object): V;

        clear(): void;

        containsValue(arg0: Object): boolean;

        keySet(): Set<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
declare module '@java/java.util.concurrent.locks' {
    import { Thread } from '@java/java.lang'
    import { Collection, Date } from '@java/java.util'
    import { Serializable } from '@java/java.io'
    import { TimeUnit } from '@java/java.util.concurrent'
    export abstract class AbstractOwnableSynchronizer implements Serializable {
    }

    export abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
        constructor();

        acquire(arg0: number): void;

        acquireInterruptibly(arg0: number): void;

        tryAcquireNanos(arg0: number, arg1: number): boolean;

        release(arg0: number): boolean;

        acquireShared(arg0: number): void;

        acquireSharedInterruptibly(arg0: number): void;

        tryAcquireSharedNanos(arg0: number, arg1: number): boolean;

        releaseShared(arg0: number): boolean;

        hasQueuedThreads(): boolean;

        hasContended(): boolean;

        getFirstQueuedThread(): Thread;

        isQueued(arg0: Thread): boolean;

        hasQueuedPredecessors(): boolean;

        getQueueLength(): number;

        getQueuedThreads(): Collection<Thread>;

        getExclusiveQueuedThreads(): Collection<Thread>;

        getSharedQueuedThreads(): Collection<Thread>;
        toString(): string;

        owns(arg0: AbstractQueuedLongSynchronizer.ConditionObject): boolean;

        hasWaiters(arg0: AbstractQueuedLongSynchronizer.ConditionObject): boolean;

        getWaitQueueLength(arg0: AbstractQueuedLongSynchronizer.ConditionObject): number;

        getWaitingThreads(arg0: AbstractQueuedLongSynchronizer.ConditionObject): Collection<Thread>;
    }
    export namespace AbstractQueuedLongSynchronizer {
        export class ConditionObject implements Condition, Serializable {
            constructor(arg0: AbstractQueuedLongSynchronizer);

            signal(): void;

            signalAll(): void;

            awaitUninterruptibly(): void;

            await(): void;

            awaitNanos(arg0: number): number;

            awaitUntil(arg0: Date): boolean;

            await(arg0: number, arg1: TimeUnit): boolean;
        }

    }

    export abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements Serializable {

        acquire(arg0: number): void;

        acquireInterruptibly(arg0: number): void;

        tryAcquireNanos(arg0: number, arg1: number): boolean;

        release(arg0: number): boolean;

        acquireShared(arg0: number): void;

        acquireSharedInterruptibly(arg0: number): void;

        tryAcquireSharedNanos(arg0: number, arg1: number): boolean;

        releaseShared(arg0: number): boolean;

        hasQueuedThreads(): boolean;

        hasContended(): boolean;

        getFirstQueuedThread(): Thread;

        isQueued(arg0: Thread): boolean;

        hasQueuedPredecessors(): boolean;

        getQueueLength(): number;

        getQueuedThreads(): Collection<Thread>;

        getExclusiveQueuedThreads(): Collection<Thread>;

        getSharedQueuedThreads(): Collection<Thread>;
        toString(): string;

        owns(arg0: AbstractQueuedSynchronizer.ConditionObject): boolean;

        hasWaiters(arg0: AbstractQueuedSynchronizer.ConditionObject): boolean;

        getWaitQueueLength(arg0: AbstractQueuedSynchronizer.ConditionObject): number;

        getWaitingThreads(arg0: AbstractQueuedSynchronizer.ConditionObject): Collection<Thread>;
    }
    export namespace AbstractQueuedSynchronizer {
        export class ConditionObject implements Condition, Serializable {
            constructor(arg0: AbstractQueuedSynchronizer);

            signal(): void;

            signalAll(): void;

            awaitUninterruptibly(): void;

            await(): void;

            awaitNanos(arg0: number): number;

            awaitUntil(arg0: Date): boolean;

            await(arg0: number, arg1: TimeUnit): boolean;
        }

    }

    export interface Condition {

        await(): void;

        awaitUninterruptibly(): void;

        awaitNanos(arg0: number): number;

        await(arg0: number, arg1: TimeUnit): boolean;

        awaitUntil(arg0: Date): boolean;

        signal(): void;

        signalAll(): void;
    }

    export interface Lock {

        lock(): void;

        lockInterruptibly(): void;

        tryLock(): boolean;

        tryLock(arg0: number, arg1: TimeUnit): boolean;

        unlock(): void;

        newCondition(): Condition;
    }

    export class LockSupport {

        static setCurrentBlocker(arg0: Object): void;

        static unpark(arg0: Thread): void;

        static park(arg0: Object): void;

        static parkNanos(arg0: Object, arg1: number): void;

        static parkUntil(arg0: Object, arg1: number): void;

        static getBlocker(arg0: Thread): Object;

        static park(): void;

        static parkNanos(arg0: number): void;

        static parkUntil(arg0: number): void;
    }

    export interface ReadWriteLock {

        readLock(): Lock;

        writeLock(): Lock;
    }

    export class ReentrantLock implements Lock, Serializable {
        constructor();
        constructor(arg0: boolean);

        lock(): void;

        lockInterruptibly(): void;

        tryLock(): boolean;

        tryLock(arg0: number, arg1: TimeUnit): boolean;

        unlock(): void;

        newCondition(): Condition;

        getHoldCount(): number;

        isHeldByCurrentThread(): boolean;

        isLocked(): boolean;

        isFair(): boolean;

        hasQueuedThreads(): boolean;

        hasQueuedThread(arg0: Thread): boolean;

        getQueueLength(): number;

        hasWaiters(arg0: Condition): boolean;

        getWaitQueueLength(arg0: Condition): number;
        toString(): string;
    }

    export class ReentrantReadWriteLock implements ReadWriteLock, Serializable {
        constructor();
        constructor(arg0: boolean);

        writeLock(): ReentrantReadWriteLock.WriteLock;

        readLock(): ReentrantReadWriteLock.ReadLock;

        isFair(): boolean;

        getReadLockCount(): number;

        isWriteLocked(): boolean;

        isWriteLockedByCurrentThread(): boolean;

        getWriteHoldCount(): number;

        getReadHoldCount(): number;

        hasQueuedThreads(): boolean;

        hasQueuedThread(arg0: Thread): boolean;

        getQueueLength(): number;

        hasWaiters(arg0: Condition): boolean;

        getWaitQueueLength(arg0: Condition): number;
        toString(): string;
    }
    export namespace ReentrantReadWriteLock {
        export class ReadLock implements Lock, Serializable {

            lock(): void;

            lockInterruptibly(): void;

            tryLock(): boolean;

            tryLock(arg0: number, arg1: TimeUnit): boolean;

            unlock(): void;

            newCondition(): Condition;
            toString(): string;
        }

        export class WriteLock implements Lock, Serializable {

            lock(): void;

            lockInterruptibly(): void;

            tryLock(): boolean;

            tryLock(arg0: number, arg1: TimeUnit): boolean;

            unlock(): void;

            newCondition(): Condition;
            toString(): string;

            isHeldByCurrentThread(): boolean;

            getHoldCount(): number;
        }

    }

    export class StampedLock implements Serializable {
        constructor();

        writeLock(): number;

        tryWriteLock(): number;

        tryWriteLock(arg0: number, arg1: TimeUnit): number;

        writeLockInterruptibly(): number;

        readLock(): number;

        tryReadLock(): number;

        tryReadLock(arg0: number, arg1: TimeUnit): number;

        readLockInterruptibly(): number;

        tryOptimisticRead(): number;

        validate(arg0: number): boolean;

        unlockWrite(arg0: number): void;

        unlockRead(arg0: number): void;

        unlock(arg0: number): void;

        tryConvertToWriteLock(arg0: number): number;

        tryConvertToReadLock(arg0: number): number;

        tryConvertToOptimisticRead(arg0: number): number;

        tryUnlockWrite(): boolean;

        tryUnlockRead(): boolean;

        isWriteLocked(): boolean;

        isReadLocked(): boolean;

        static isWriteLockStamp(arg0: number): boolean;

        static isReadLockStamp(arg0: number): boolean;

        static isLockStamp(arg0: number): boolean;

        static isOptimisticReadStamp(arg0: number): boolean;

        getReadLockCount(): number;
        toString(): string;

        asReadLock(): Lock;

        asWriteLock(): Lock;

        asReadWriteLock(): ReadWriteLock;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.time.temporal.d.ts" />
declare module '@java/java.util.concurrent' {
    import { PrivilegedAction, PrivilegedExceptionAction } from '@java/java.security'
    import { Enum, IllegalStateException, Comparable, AutoCloseable, String, Exception, Thread, RuntimeException, Runnable, Throwable, Cloneable, Class, Void, Boolean } from '@java/java.lang'
    import { NavigableSet, Set, Enumeration, NavigableMap, AbstractCollection, RandomAccess, AbstractSet, ListIterator, Deque, SortedSet, Comparator, AbstractMap, AbstractQueue, Random, SortedMap, Iterator, Collection, List, Queue, Map, Spliterator } from '@java/java.util'
    import { Duration } from '@java/java.time'
    import { Serializable } from '@java/java.io'
    import { IntStream, DoubleStream, LongStream } from '@java/java.util.stream'
    import { LongBinaryOperator, ToDoubleBiFunction, Predicate, Function, ToLongFunction, DoubleBinaryOperator, Consumer, BiFunction, ToIntBiFunction, IntBinaryOperator, Supplier, ToIntFunction, UnaryOperator, ToLongBiFunction, BiConsumer, BiPredicate, ToDoubleFunction } from '@java/java.util.function'
    import { ChronoUnit } from '@java/java.time.temporal'
    export abstract class AbstractExecutorService implements ExecutorService {
        constructor();

        submit(arg0: Runnable): Future<any>;

        submit<T extends Object>(arg0: Runnable, arg1: T): Future<T>;

        submit<T extends Object>(arg0: Callable<T>): Future<T>;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>): T;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): T;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>): List<Future<T>>;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): List<Future<T>>;
    }

    export interface ArrayBlockingQueue<E extends Object> extends BlockingQueue<E>, Serializable { }
    export class ArrayBlockingQueue<E extends Object> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
        constructor(arg0: number);
        constructor(arg0: number, arg1: boolean);
        constructor(arg0: number, arg1: boolean, arg2: Collection<E>);

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        poll(): E;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        peek(): E;

        size(): number;

        remainingCapacity(): number;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];
        toString(): string;

        clear(): void;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        iterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        forEach(arg0: Consumer<E>): void;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;
    }

    export interface BlockingDeque<E extends Object> extends BlockingQueue<E>, Deque<E>, Object {

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        putFirst(arg0: E): void;

        putLast(arg0: E): void;

        offerFirst(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        offerLast(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        takeFirst(): E;

        takeLast(): E;

        pollFirst(arg0: number, arg1: TimeUnit): E;

        pollLast(arg0: number, arg1: TimeUnit): E;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        remove(): E;

        poll(): E;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        element(): E;

        peek(): E;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        size(): number;

        iterator(): Iterator<E>;

        push(arg0: E): void;
    }

    export interface BlockingQueue<E extends Object> extends Queue<E>, Object {

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        remainingCapacity(): number;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;
    }

    export class BrokenBarrierException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface Callable<V extends Object> extends Object {

        call(): V;
    }

    export class CancellationException extends IllegalStateException {
        constructor();
        constructor(arg0: String);
    }

    export class CompletableFuture<T extends Object> extends Object implements Future<T>, CompletionStage<T> {
        constructor();

        static supplyAsync<U extends Object>(arg0: Supplier<U>): CompletableFuture<U>;

        static supplyAsync<U extends Object>(arg0: Supplier<U>, arg1: Executor): CompletableFuture<U>;

        static runAsync(arg0: Runnable): CompletableFuture<Void>;

        static runAsync(arg0: Runnable, arg1: Executor): CompletableFuture<Void>;

        static completedFuture<U extends Object>(arg0: U): CompletableFuture<U>;

        isDone(): boolean;

        get(): T;

        get(arg0: number, arg1: TimeUnit): T;

        join(): T;

        getNow(arg0: T): T;

        complete(arg0: T): boolean;

        completeExceptionally(arg0: Throwable): boolean;

        thenApply<U extends Object>(arg0: Function<T, U>): CompletableFuture<U>;

        thenApplyAsync<U extends Object>(arg0: Function<T, U>): CompletableFuture<U>;

        thenApplyAsync<U extends Object>(arg0: Function<T, U>, arg1: Executor): CompletableFuture<U>;

        thenAccept(arg0: Consumer<T>): CompletableFuture<Void>;

        thenAcceptAsync(arg0: Consumer<T>): CompletableFuture<Void>;

        thenAcceptAsync(arg0: Consumer<T>, arg1: Executor): CompletableFuture<Void>;

        thenRun(arg0: Runnable): CompletableFuture<Void>;

        thenRunAsync(arg0: Runnable): CompletableFuture<Void>;

        thenRunAsync(arg0: Runnable, arg1: Executor): CompletableFuture<Void>;

        thenCombine<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>): CompletableFuture<V>;

        thenCombineAsync<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>): CompletableFuture<V>;

        thenCombineAsync<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>, arg2: Executor): CompletableFuture<V>;

        thenAcceptBoth<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>): CompletableFuture<Void>;

        thenAcceptBothAsync<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>): CompletableFuture<Void>;

        thenAcceptBothAsync<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>, arg2: Executor): CompletableFuture<Void>;

        runAfterBoth(arg0: CompletionStage<any>, arg1: Runnable): CompletableFuture<Void>;

        runAfterBothAsync(arg0: CompletionStage<any>, arg1: Runnable): CompletableFuture<Void>;

        runAfterBothAsync(arg0: CompletionStage<any>, arg1: Runnable, arg2: Executor): CompletableFuture<Void>;

        applyToEither<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>): CompletableFuture<U>;

        applyToEitherAsync<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>): CompletableFuture<U>;

        applyToEitherAsync<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>, arg2: Executor): CompletableFuture<U>;

        acceptEither(arg0: CompletionStage<T>, arg1: Consumer<T>): CompletableFuture<Void>;

        acceptEitherAsync(arg0: CompletionStage<T>, arg1: Consumer<T>): CompletableFuture<Void>;

        acceptEitherAsync(arg0: CompletionStage<T>, arg1: Consumer<T>, arg2: Executor): CompletableFuture<Void>;

        runAfterEither(arg0: CompletionStage<any>, arg1: Runnable): CompletableFuture<Void>;

        runAfterEitherAsync(arg0: CompletionStage<any>, arg1: Runnable): CompletableFuture<Void>;

        runAfterEitherAsync(arg0: CompletionStage<any>, arg1: Runnable, arg2: Executor): CompletableFuture<Void>;

        thenCompose<U extends Object>(arg0: Function<T, CompletionStage<U>>): CompletableFuture<U>;

        thenComposeAsync<U extends Object>(arg0: Function<T, CompletionStage<U>>): CompletableFuture<U>;

        thenComposeAsync<U extends Object>(arg0: Function<T, CompletionStage<U>>, arg1: Executor): CompletableFuture<U>;

        whenComplete(arg0: BiConsumer<T, Throwable>): CompletableFuture<T>;

        whenCompleteAsync(arg0: BiConsumer<T, Throwable>): CompletableFuture<T>;

        whenCompleteAsync(arg0: BiConsumer<T, Throwable>, arg1: Executor): CompletableFuture<T>;

        handle<U extends Object>(arg0: BiFunction<T, Throwable, U>): CompletableFuture<U>;

        handleAsync<U extends Object>(arg0: BiFunction<T, Throwable, U>): CompletableFuture<U>;

        handleAsync<U extends Object>(arg0: BiFunction<T, Throwable, U>, arg1: Executor): CompletableFuture<U>;

        toCompletableFuture(): CompletableFuture<T>;

        exceptionally(arg0: Function<Throwable, T>): CompletableFuture<T>;

        exceptionallyAsync(arg0: Function<Throwable, T>): CompletableFuture<T>;

        exceptionallyAsync(arg0: Function<Throwable, T>, arg1: Executor): CompletableFuture<T>;

        exceptionallyCompose(arg0: Function<Throwable, CompletionStage<T>>): CompletableFuture<T>;

        exceptionallyComposeAsync(arg0: Function<Throwable, CompletionStage<T>>): CompletableFuture<T>;

        exceptionallyComposeAsync(arg0: Function<Throwable, CompletionStage<T>>, arg1: Executor): CompletableFuture<T>;

        static allOf(arg0: CompletableFuture<any>[]): CompletableFuture<Void>;

        static anyOf(arg0: CompletableFuture<any>[]): CompletableFuture<Object>;

        cancel(arg0: boolean): boolean;

        isCancelled(): boolean;

        isCompletedExceptionally(): boolean;

        obtrudeValue(arg0: T): void;

        obtrudeException(arg0: Throwable): void;

        getNumberOfDependents(): number;
        toString(): string;

        newIncompleteFuture<U extends Object>(): CompletableFuture<U>;

        defaultExecutor(): Executor;

        copy(): CompletableFuture<T>;

        minimalCompletionStage(): CompletionStage<T>;

        completeAsync(arg0: Supplier<T>, arg1: Executor): CompletableFuture<T>;

        completeAsync(arg0: Supplier<T>): CompletableFuture<T>;

        orTimeout(arg0: number, arg1: TimeUnit): CompletableFuture<T>;

        completeOnTimeout(arg0: T, arg1: number, arg2: TimeUnit): CompletableFuture<T>;

        static delayedExecutor(arg0: number, arg1: TimeUnit, arg2: Executor): Executor;

        static delayedExecutor(arg0: number, arg1: TimeUnit): Executor;

        static completedStage<U extends Object>(arg0: U): CompletionStage<U>;

        static failedFuture<U extends Object>(arg0: Throwable): CompletableFuture<U>;

        static failedStage<U extends Object>(arg0: Throwable): CompletionStage<U>;
    }
    export namespace CompletableFuture {
        export interface AsynchronousCompletionTask {
        }

    }

    export class CompletionException extends RuntimeException {
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export interface CompletionService<V extends Object> extends Object {

        submit(arg0: Callable<V>): Future<V>;

        submit(arg0: Runnable, arg1: V): Future<V>;

        take(): Future<V>;

        poll(): Future<V>;

        poll(arg0: number, arg1: TimeUnit): Future<V>;
    }

    export interface CompletionStage<T extends Object> extends Object {

        thenApply<U extends Object>(arg0: Function<T, U>): CompletionStage<U>;

        thenApplyAsync<U extends Object>(arg0: Function<T, U>): CompletionStage<U>;

        thenApplyAsync<U extends Object>(arg0: Function<T, U>, arg1: Executor): CompletionStage<U>;

        thenAccept(arg0: Consumer<T>): CompletionStage<Void>;

        thenAcceptAsync(arg0: Consumer<T>): CompletionStage<Void>;

        thenAcceptAsync(arg0: Consumer<T>, arg1: Executor): CompletionStage<Void>;

        thenRun(arg0: Runnable): CompletionStage<Void>;

        thenRunAsync(arg0: Runnable): CompletionStage<Void>;

        thenRunAsync(arg0: Runnable, arg1: Executor): CompletionStage<Void>;

        thenCombine<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>): CompletionStage<V>;

        thenCombineAsync<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>): CompletionStage<V>;

        thenCombineAsync<U extends Object, V extends Object>(arg0: CompletionStage<U>, arg1: BiFunction<T, U, V>, arg2: Executor): CompletionStage<V>;

        thenAcceptBoth<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>): CompletionStage<Void>;

        thenAcceptBothAsync<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>): CompletionStage<Void>;

        thenAcceptBothAsync<U extends Object>(arg0: CompletionStage<U>, arg1: BiConsumer<T, U>, arg2: Executor): CompletionStage<Void>;

        runAfterBoth(arg0: CompletionStage<any>, arg1: Runnable): CompletionStage<Void>;

        runAfterBothAsync(arg0: CompletionStage<any>, arg1: Runnable): CompletionStage<Void>;

        runAfterBothAsync(arg0: CompletionStage<any>, arg1: Runnable, arg2: Executor): CompletionStage<Void>;

        applyToEither<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>): CompletionStage<U>;

        applyToEitherAsync<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>): CompletionStage<U>;

        applyToEitherAsync<U extends Object>(arg0: CompletionStage<T>, arg1: Function<T, U>, arg2: Executor): CompletionStage<U>;

        acceptEither(arg0: CompletionStage<T>, arg1: Consumer<T>): CompletionStage<Void>;

        acceptEitherAsync(arg0: CompletionStage<T>, arg1: Consumer<T>): CompletionStage<Void>;

        acceptEitherAsync(arg0: CompletionStage<T>, arg1: Consumer<T>, arg2: Executor): CompletionStage<Void>;

        runAfterEither(arg0: CompletionStage<any>, arg1: Runnable): CompletionStage<Void>;

        runAfterEitherAsync(arg0: CompletionStage<any>, arg1: Runnable): CompletionStage<Void>;

        runAfterEitherAsync(arg0: CompletionStage<any>, arg1: Runnable, arg2: Executor): CompletionStage<Void>;

        thenCompose<U extends Object>(arg0: Function<T, CompletionStage<U>>): CompletionStage<U>;

        thenComposeAsync<U extends Object>(arg0: Function<T, CompletionStage<U>>): CompletionStage<U>;

        thenComposeAsync<U extends Object>(arg0: Function<T, CompletionStage<U>>, arg1: Executor): CompletionStage<U>;

        handle<U extends Object>(arg0: BiFunction<T, Throwable, U>): CompletionStage<U>;

        handleAsync<U extends Object>(arg0: BiFunction<T, Throwable, U>): CompletionStage<U>;

        handleAsync<U extends Object>(arg0: BiFunction<T, Throwable, U>, arg1: Executor): CompletionStage<U>;

        whenComplete(arg0: BiConsumer<T, Throwable>): CompletionStage<T>;

        whenCompleteAsync(arg0: BiConsumer<T, Throwable>): CompletionStage<T>;

        whenCompleteAsync(arg0: BiConsumer<T, Throwable>, arg1: Executor): CompletionStage<T>;

        exceptionally(arg0: Function<Throwable, T>): CompletionStage<T>;

/* default */ exceptionallyAsync(arg0: Function<Throwable, T>): CompletionStage<T>;

/* default */ exceptionallyAsync(arg0: Function<Throwable, T>, arg1: Executor): CompletionStage<T>;

/* default */ exceptionallyCompose(arg0: Function<Throwable, CompletionStage<T>>): CompletionStage<T>;

/* default */ exceptionallyComposeAsync(arg0: Function<Throwable, CompletionStage<T>>): CompletionStage<T>;

/* default */ exceptionallyComposeAsync(arg0: Function<Throwable, CompletionStage<T>>, arg1: Executor): CompletionStage<T>;

        toCompletableFuture(): CompletableFuture<T>;
    }

    export class ConcurrentHashMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Map<K, V>);
        constructor(arg0: number, arg1: number);
        constructor(arg0: number, arg1: number, arg2: number);

        size(): number;

        isEmpty(): boolean;

        get(arg0: Object): V;

        containsKey(arg0: Object): boolean;

        containsValue(arg0: Object): boolean;

        put(arg0: K, arg1: V): V;

        putAll(arg0: Map<K, V>): void;

        remove(arg0: Object): V;

        clear(): void;

        keySet(): ConcurrentHashMap.KeySetView<K, V>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        hashCode(): number;
        toString(): string;

        equals(arg0: Object): boolean;

        putIfAbsent(arg0: K, arg1: V): V;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

        getOrDefault(arg0: Object, arg1: V): V;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;

        computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

        computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

        compute(arg0: K, arg1: BiFunction<K, V, V>): V;

        merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;

        contains(arg0: Object): boolean;

        keys(): Enumeration<K>;

        elements(): Enumeration<V>;

        mappingCount(): number;

        static newKeySet<K extends Object>(): ConcurrentHashMap.KeySetView<K, Boolean>;

        static newKeySet<K extends Object>(arg0: number): ConcurrentHashMap.KeySetView<K, Boolean>;

        keySet(arg0: V): ConcurrentHashMap.KeySetView<K, V>;

        forEach(arg0: number, arg1: BiConsumer<K, V>): void;

        forEach<U extends Object>(arg0: number, arg1: BiFunction<K, V, U>, arg2: Consumer<U>): void;

        search<U extends Object>(arg0: number, arg1: BiFunction<K, V, U>): U;

        reduce<U extends Object>(arg0: number, arg1: BiFunction<K, V, U>, arg2: BiFunction<U, U, U>): U;

        reduceToDouble(arg0: number, arg1: ToDoubleBiFunction<K, V>, arg2: number, arg3: DoubleBinaryOperator): number;

        reduceToLong(arg0: number, arg1: ToLongBiFunction<K, V>, arg2: number, arg3: LongBinaryOperator): number;

        reduceToInt(arg0: number, arg1: ToIntBiFunction<K, V>, arg2: number, arg3: IntBinaryOperator): number;

        forEachKey(arg0: number, arg1: Consumer<K>): void;

        forEachKey<U extends Object>(arg0: number, arg1: Function<K, U>, arg2: Consumer<U>): void;

        searchKeys<U extends Object>(arg0: number, arg1: Function<K, U>): U;

        reduceKeys(arg0: number, arg1: BiFunction<K, K, K>): K;

        reduceKeys<U extends Object>(arg0: number, arg1: Function<K, U>, arg2: BiFunction<U, U, U>): U;

        reduceKeysToDouble(arg0: number, arg1: ToDoubleFunction<K>, arg2: number, arg3: DoubleBinaryOperator): number;

        reduceKeysToLong(arg0: number, arg1: ToLongFunction<K>, arg2: number, arg3: LongBinaryOperator): number;

        reduceKeysToInt(arg0: number, arg1: ToIntFunction<K>, arg2: number, arg3: IntBinaryOperator): number;

        forEachValue(arg0: number, arg1: Consumer<V>): void;

        forEachValue<U extends Object>(arg0: number, arg1: Function<V, U>, arg2: Consumer<U>): void;

        searchValues<U extends Object>(arg0: number, arg1: Function<V, U>): U;

        reduceValues(arg0: number, arg1: BiFunction<V, V, V>): V;

        reduceValues<U extends Object>(arg0: number, arg1: Function<V, U>, arg2: BiFunction<U, U, U>): U;

        reduceValuesToDouble(arg0: number, arg1: ToDoubleFunction<V>, arg2: number, arg3: DoubleBinaryOperator): number;

        reduceValuesToLong(arg0: number, arg1: ToLongFunction<V>, arg2: number, arg3: LongBinaryOperator): number;

        reduceValuesToInt(arg0: number, arg1: ToIntFunction<V>, arg2: number, arg3: IntBinaryOperator): number;

        forEachEntry(arg0: number, arg1: Consumer<Map.Entry<K, V>>): void;

        forEachEntry<U extends Object>(arg0: number, arg1: Function<Map.Entry<K, V>, U>, arg2: Consumer<U>): void;

        searchEntries<U extends Object>(arg0: number, arg1: Function<Map.Entry<K, V>, U>): U;

        reduceEntries(arg0: number, arg1: BiFunction<Map.Entry<K, V>, Map.Entry<K, V>, Map.Entry<K, V>>): Map.Entry<K, V>;

        reduceEntries<U extends Object>(arg0: number, arg1: Function<Map.Entry<K, V>, U>, arg2: BiFunction<U, U, U>): U;

        reduceEntriesToDouble(arg0: number, arg1: ToDoubleFunction<Map.Entry<K, V>>, arg2: number, arg3: DoubleBinaryOperator): number;

        reduceEntriesToLong(arg0: number, arg1: ToLongFunction<Map.Entry<K, V>>, arg2: number, arg3: LongBinaryOperator): number;

        reduceEntriesToInt(arg0: number, arg1: ToIntFunction<Map.Entry<K, V>>, arg2: number, arg3: IntBinaryOperator): number;
    }
    export namespace ConcurrentHashMap {
        export interface KeySetView<K extends Object, V extends Object> extends Set<K>, Serializable { }
        export class KeySetView<K extends Object, V extends Object> extends ConcurrentHashMap.CollectionView<K, V, K> implements Set<K>, Serializable {

            getMappedValue(): V;

            contains(arg0: Object): boolean;

            remove(arg0: Object): boolean;

            iterator(): Iterator<K>;

            add(arg0: K): boolean;

            addAll(arg0: Collection<K>): boolean;

            hashCode(): number;

            equals(arg0: Object): boolean;

            spliterator(): Spliterator<K>;

            forEach(arg0: Consumer<K>): void;
        }

    }

    export interface ConcurrentLinkedDeque<E extends Object> extends Deque<E>, Serializable { }
    export class ConcurrentLinkedDeque<E extends Object> extends AbstractCollection<E> implements Deque<E>, Serializable {
        constructor();
        constructor(arg0: Collection<E>);

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        peekFirst(): E;

        peekLast(): E;

        getFirst(): E;

        getLast(): E;

        pollFirst(): E;

        pollLast(): E;

        removeFirst(): E;

        removeLast(): E;

        offer(arg0: E): boolean;

        add(arg0: E): boolean;

        poll(): E;

        peek(): E;

        remove(): E;

        pop(): E;

        element(): E;

        push(arg0: E): void;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        isEmpty(): boolean;

        size(): number;

        remove(arg0: Object): boolean;

        addAll(arg0: Collection<E>): boolean;

        clear(): void;
        toString(): string;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        forEach(arg0: Consumer<E>): void;
    }

    export interface ConcurrentLinkedQueue<E extends Object> extends Queue<E>, Serializable { }
    export class ConcurrentLinkedQueue<E extends Object> extends AbstractQueue<E> implements Queue<E>, Serializable {
        constructor();
        constructor(arg0: Collection<E>);

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        poll(): E;

        peek(): E;

        isEmpty(): boolean;

        size(): number;

        contains(arg0: Object): boolean;

        remove(arg0: Object): boolean;

        addAll(arg0: Collection<E>): boolean;
        toString(): string;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        iterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        clear(): void;

        forEach(arg0: Consumer<E>): void;
    }

    export interface ConcurrentMap<K extends Object, V extends Object> extends Map<K, V>, Object {

/* default */ getOrDefault(arg0: Object, arg1: V): V;

/* default */ forEach(arg0: BiConsumer<K, V>): void;

        putIfAbsent(arg0: K, arg1: V): V;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

/* default */ replaceAll(arg0: BiFunction<K, V, V>): void;

/* default */ computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

/* default */ computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

/* default */ compute(arg0: K, arg1: BiFunction<K, V, V>): V;

/* default */ merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;
    }

    export interface ConcurrentNavigableMap<K extends Object, V extends Object> extends ConcurrentMap<K, V>, NavigableMap<K, V>, Object {

        subMap(arg0: K, arg1: boolean, arg2: K, arg3: boolean): ConcurrentNavigableMap<K, V>;

        headMap(arg0: K, arg1: boolean): ConcurrentNavigableMap<K, V>;

        tailMap(arg0: K, arg1: boolean): ConcurrentNavigableMap<K, V>;

        subMap(arg0: K, arg1: K): ConcurrentNavigableMap<K, V>;

        headMap(arg0: K): ConcurrentNavigableMap<K, V>;

        tailMap(arg0: K): ConcurrentNavigableMap<K, V>;

        descendingMap(): ConcurrentNavigableMap<K, V>;

        navigableKeySet(): NavigableSet<K>;

        keySet(): NavigableSet<K>;

        descendingKeySet(): NavigableSet<K>;
    }

    export class ConcurrentSkipListMap<K extends Object, V extends Object> extends AbstractMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Comparator<K>);
        constructor(arg0: Map<K, V>);
        constructor(arg0: SortedMap<K, V>);

        clone(): ConcurrentSkipListMap<K, V>;

        containsKey(arg0: Object): boolean;

        get(arg0: Object): V;

        getOrDefault(arg0: Object, arg1: V): V;

        put(arg0: K, arg1: V): V;

        remove(arg0: Object): V;

        containsValue(arg0: Object): boolean;

        size(): number;

        isEmpty(): boolean;

        clear(): void;

        computeIfAbsent(arg0: K, arg1: Function<K, V>): V;

        computeIfPresent(arg0: K, arg1: BiFunction<K, V, V>): V;

        compute(arg0: K, arg1: BiFunction<K, V, V>): V;

        merge(arg0: K, arg1: V, arg2: BiFunction<V, V, V>): V;

        keySet(): NavigableSet<K>;

        navigableKeySet(): NavigableSet<K>;

        values(): Collection<V>;

        entrySet(): Set<Map.Entry<K, V>>;

        descendingMap(): ConcurrentNavigableMap<K, V>;

        descendingKeySet(): NavigableSet<K>;

        equals(arg0: Object): boolean;

        putIfAbsent(arg0: K, arg1: V): V;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: K, arg1: V, arg2: V): boolean;

        replace(arg0: K, arg1: V): V;

        comparator(): Comparator<K>;

        firstKey(): K;

        lastKey(): K;

        subMap(arg0: K, arg1: boolean, arg2: K, arg3: boolean): ConcurrentNavigableMap<K, V>;

        headMap(arg0: K, arg1: boolean): ConcurrentNavigableMap<K, V>;

        tailMap(arg0: K, arg1: boolean): ConcurrentNavigableMap<K, V>;

        subMap(arg0: K, arg1: K): ConcurrentNavigableMap<K, V>;

        headMap(arg0: K): ConcurrentNavigableMap<K, V>;

        tailMap(arg0: K): ConcurrentNavigableMap<K, V>;

        lowerEntry(arg0: K): Map.Entry<K, V>;

        lowerKey(arg0: K): K;

        floorEntry(arg0: K): Map.Entry<K, V>;

        floorKey(arg0: K): K;

        ceilingEntry(arg0: K): Map.Entry<K, V>;

        ceilingKey(arg0: K): K;

        higherEntry(arg0: K): Map.Entry<K, V>;

        higherKey(arg0: K): K;

        firstEntry(): Map.Entry<K, V>;

        lastEntry(): Map.Entry<K, V>;

        pollFirstEntry(): Map.Entry<K, V>;

        pollLastEntry(): Map.Entry<K, V>;

        forEach(arg0: BiConsumer<K, V>): void;

        replaceAll(arg0: BiFunction<K, V, V>): void;
    }

    export interface ConcurrentSkipListSet<E extends Object> extends NavigableSet<E>, Cloneable, Serializable { }
    export class ConcurrentSkipListSet<E extends Object> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable {
        constructor();
        constructor(arg0: Comparator<E>);
        constructor(arg0: Collection<E>);
        constructor(arg0: SortedSet<E>);

        clone(): ConcurrentSkipListSet<E>;

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        add(arg0: E): boolean;

        remove(arg0: Object): boolean;

        clear(): void;

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;

        equals(arg0: Object): boolean;

        removeAll(arg0: Collection<any>): boolean;

        lower(arg0: E): E;

        floor(arg0: E): E;

        ceiling(arg0: E): E;

        higher(arg0: E): E;

        pollFirst(): E;

        pollLast(): E;

        comparator(): Comparator<E>;

        first(): E;

        last(): E;

        subSet(arg0: E, arg1: boolean, arg2: E, arg3: boolean): NavigableSet<E>;

        headSet(arg0: E, arg1: boolean): NavigableSet<E>;

        tailSet(arg0: E, arg1: boolean): NavigableSet<E>;

        subSet(arg0: E, arg1: E): NavigableSet<E>;

        headSet(arg0: E): NavigableSet<E>;

        tailSet(arg0: E): NavigableSet<E>;

        descendingSet(): NavigableSet<E>;

        spliterator(): Spliterator<E>;
    }

    export interface CopyOnWriteArrayList<E extends Object> extends List<E>, RandomAccess, Cloneable, Serializable { }
    export class CopyOnWriteArrayList<E extends Object> extends Object implements List<E>, RandomAccess, Cloneable, Serializable {
        constructor();
        constructor(arg0: Collection<E>);
        constructor(arg0: E[]);

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        indexOf(arg0: Object): number;

        indexOf(arg0: E, arg1: number): number;

        lastIndexOf(arg0: Object): number;

        lastIndexOf(arg0: E, arg1: number): number;

        clone(): Object;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        get(arg0: number): E;

        set(arg0: number, arg1: E): E;

        add(arg0: E): boolean;

        add(arg0: number, arg1: E): void;

        remove(arg0: number): E;

        remove(arg0: Object): boolean;

        addIfAbsent(arg0: E): boolean;

        containsAll(arg0: Collection<any>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        addAllAbsent(arg0: Collection<E>): number;

        clear(): void;

        addAll(arg0: Collection<E>): boolean;

        addAll(arg0: number, arg1: Collection<E>): boolean;

        forEach(arg0: Consumer<E>): void;

        removeIf(arg0: Predicate<E>): boolean;

        replaceAll(arg0: UnaryOperator<E>): void;

        sort(arg0: Comparator<E>): void;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;

        iterator(): Iterator<E>;

        listIterator(): ListIterator<E>;

        listIterator(arg0: number): ListIterator<E>;

        spliterator(): Spliterator<E>;

        subList(arg0: number, arg1: number): List<E>;
    }

    export interface CopyOnWriteArraySet<E extends Object> extends Serializable { }
    export class CopyOnWriteArraySet<E extends Object> extends AbstractSet<E> implements Serializable {
        constructor();
        constructor(arg0: Collection<E>);

        size(): number;

        isEmpty(): boolean;

        contains(arg0: Object): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        clear(): void;

        remove(arg0: Object): boolean;

        add(arg0: E): boolean;

        containsAll(arg0: Collection<any>): boolean;

        addAll(arg0: Collection<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        iterator(): Iterator<E>;

        equals(arg0: Object): boolean;

        removeIf(arg0: Predicate<E>): boolean;

        forEach(arg0: Consumer<E>): void;

        spliterator(): Spliterator<E>;
    }

    export class CountDownLatch {
        constructor(arg0: number);

        await(): void;

        await(arg0: number, arg1: TimeUnit): boolean;

        countDown(): void;

        getCount(): number;
        toString(): string;
    }

    export abstract class CountedCompleter<T extends Object> extends ForkJoinTask<T> {

        abstract compute(): void;

        onCompletion(arg0: CountedCompleter<any>): void;

        onExceptionalCompletion(arg0: Throwable, arg1: CountedCompleter<any>): boolean;

        getCompleter(): CountedCompleter<any>;

        getPendingCount(): number;

        setPendingCount(arg0: number): void;

        addToPendingCount(arg0: number): void;

        compareAndSetPendingCount(arg0: number, arg1: number): boolean;

        decrementPendingCountUnlessZero(): number;

        getRoot(): CountedCompleter<any>;

        tryComplete(): void;

        propagateCompletion(): void;

        complete(arg0: T): void;

        firstComplete(): CountedCompleter<any>;

        nextComplete(): CountedCompleter<any>;

        quietlyCompleteRoot(): void;

        helpComplete(arg0: number): void;

        getRawResult(): T;
    }

    export class CyclicBarrier {
        constructor(arg0: number, arg1: Runnable);
        constructor(arg0: number);

        getParties(): number;

        await(): number;

        await(arg0: number, arg1: TimeUnit): number;

        isBroken(): boolean;

        reset(): void;

        getNumberWaiting(): number;
    }

    export interface DelayQueue<E extends Delayed> extends BlockingQueue<E> { }
    export class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
        constructor();
        constructor(arg0: Collection<E>);

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        poll(): E;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        peek(): E;

        size(): number;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        clear(): void;

        remainingCapacity(): number;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        remove(arg0: Object): boolean;

        iterator(): Iterator<E>;
    }

    export interface Delayed extends Comparable<Delayed>, Object {

        getDelay(arg0: TimeUnit): number;
    }

    export class Exchanger<V extends Object> extends Object {
        constructor();

        exchange(arg0: V): V;

        exchange(arg0: V, arg1: number, arg2: TimeUnit): V;
    }

    export class ExecutionException extends Exception {
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export interface Executor {

        execute(arg0: Runnable): void;
    }

    export class ExecutorCompletionService<V extends Object> extends Object implements CompletionService<V> {
        constructor(arg0: Executor);
        constructor(arg0: Executor, arg1: BlockingQueue<Future<V>>);

        submit(arg0: Callable<V>): Future<V>;

        submit(arg0: Runnable, arg1: V): Future<V>;

        take(): Future<V>;

        poll(): Future<V>;

        poll(arg0: number, arg1: TimeUnit): Future<V>;
    }

    export interface ExecutorService extends Executor {

        shutdown(): void;

        shutdownNow(): List<Runnable>;

        isShutdown(): boolean;

        isTerminated(): boolean;

        awaitTermination(arg0: number, arg1: TimeUnit): boolean;

        submit<T extends Object>(arg0: Callable<T>): Future<T>;

        submit<T extends Object>(arg0: Runnable, arg1: T): Future<T>;

        submit(arg0: Runnable): Future<any>;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>): List<Future<T>>;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): List<Future<T>>;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>): T;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): T;
    }

    export class Executors {

        static newFixedThreadPool(arg0: number): ExecutorService;

        static newWorkStealingPool(arg0: number): ExecutorService;

        static newWorkStealingPool(): ExecutorService;

        static newFixedThreadPool(arg0: number, arg1: ThreadFactory): ExecutorService;

        static newSingleThreadExecutor(): ExecutorService;

        static newSingleThreadExecutor(arg0: ThreadFactory): ExecutorService;

        static newCachedThreadPool(): ExecutorService;

        static newCachedThreadPool(arg0: ThreadFactory): ExecutorService;

        static newSingleThreadScheduledExecutor(): ScheduledExecutorService;

        static newSingleThreadScheduledExecutor(arg0: ThreadFactory): ScheduledExecutorService;

        static newScheduledThreadPool(arg0: number): ScheduledExecutorService;

        static newScheduledThreadPool(arg0: number, arg1: ThreadFactory): ScheduledExecutorService;

        static unconfigurableExecutorService(arg0: ExecutorService): ExecutorService;

        static unconfigurableScheduledExecutorService(arg0: ScheduledExecutorService): ScheduledExecutorService;

        static defaultThreadFactory(): ThreadFactory;

        static privilegedThreadFactory(): ThreadFactory;

        static callable<T extends Object>(arg0: Runnable, arg1: T): Callable<T>;

        static callable(arg0: Runnable): Callable<Object>;

        static callable(arg0: PrivilegedAction<any>): Callable<Object>;

        static callable(arg0: PrivilegedExceptionAction<any>): Callable<Object>;

        static privilegedCallable<T extends Object>(arg0: Callable<T>): Callable<T>;

        static privilegedCallableUsingCurrentClassLoader<T extends Object>(arg0: Callable<T>): Callable<T>;
    }

    export class Flow {

        static defaultBufferSize(): number;
    }
    export namespace Flow {
        export interface Processor<T extends Object, R extends Object> extends Flow.Subscriber<T>, Flow.Publisher<R>, Object {
        }

        export interface Publisher<T extends Object> extends Object {

            subscribe(arg0: Flow.Subscriber<T>): void;
        }

        export interface Subscriber<T extends Object> extends Object {

            onSubscribe(arg0: Flow.Subscription): void;

            onNext(arg0: T): void;

            onError(arg0: Throwable): void;

            onComplete(): void;
        }

        export interface Subscription {

            request(arg0: number): void;

            cancel(): void;
        }

    }

    export class ForkJoinPool extends AbstractExecutorService {
        static defaultForkJoinWorkerThreadFactory: ForkJoinPool.ForkJoinWorkerThreadFactory
        constructor();
        constructor(arg0: number);
        constructor(arg0: number, arg1: ForkJoinPool.ForkJoinWorkerThreadFactory, arg2: Thread.UncaughtExceptionHandler, arg3: boolean);
        constructor(arg0: number, arg1: ForkJoinPool.ForkJoinWorkerThreadFactory, arg2: Thread.UncaughtExceptionHandler, arg3: boolean, arg4: number, arg5: number, arg6: number, arg7: Predicate<ForkJoinPool>, arg8: number, arg9: TimeUnit);

        static commonPool(): ForkJoinPool;

        invoke<T extends Object>(arg0: ForkJoinTask<T>): T;

        execute(arg0: ForkJoinTask<any>): void;

        execute(arg0: Runnable): void;

        submit<T extends Object>(arg0: ForkJoinTask<T>): ForkJoinTask<T>;

        submit<T extends Object>(arg0: Callable<T>): ForkJoinTask<T>;

        submit<T extends Object>(arg0: Runnable, arg1: T): ForkJoinTask<T>;

        submit(arg0: Runnable): ForkJoinTask<any>;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>): List<Future<T>>;

        invokeAll<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): List<Future<T>>;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>): T;

        invokeAny<T extends Object>(arg0: Collection<Callable<T>>, arg1: number, arg2: TimeUnit): T;

        getFactory(): ForkJoinPool.ForkJoinWorkerThreadFactory;

        getUncaughtExceptionHandler(): Thread.UncaughtExceptionHandler;

        getParallelism(): number;

        static getCommonPoolParallelism(): number;

        getPoolSize(): number;

        getAsyncMode(): boolean;

        getRunningThreadCount(): number;

        getActiveThreadCount(): number;

        isQuiescent(): boolean;

        getStealCount(): number;

        getQueuedTaskCount(): number;

        getQueuedSubmissionCount(): number;

        hasQueuedSubmissions(): boolean;
        toString(): string;

        shutdown(): void;

        shutdownNow(): List<Runnable>;

        isTerminated(): boolean;

        isTerminating(): boolean;

        isShutdown(): boolean;

        awaitTermination(arg0: number, arg1: TimeUnit): boolean;

        awaitQuiescence(arg0: number, arg1: TimeUnit): boolean;

        static managedBlock(arg0: ForkJoinPool.ManagedBlocker): void;
    }
    export namespace ForkJoinPool {
        export interface ForkJoinWorkerThreadFactory {

            newThread(arg0: ForkJoinPool): ForkJoinWorkerThread;
        }

        export interface ManagedBlocker {

            block(): boolean;

            isReleasable(): boolean;
        }

    }

    export abstract class ForkJoinTask<V extends Object> extends Object implements Future<V>, Serializable {
        constructor();

        fork(): ForkJoinTask<V>;

        join(): V;

        invoke(): V;

        static invokeAll(arg0: ForkJoinTask<any>, arg1: ForkJoinTask<any>): void;

        static invokeAll(arg0: ForkJoinTask<any>[]): void;

        static invokeAll<T extends ForkJoinTask<any>>(arg0: Collection<T>): Collection<T>;

        cancel(arg0: boolean): boolean;

        isDone(): boolean;

        isCancelled(): boolean;

        isCompletedAbnormally(): boolean;

        isCompletedNormally(): boolean;

        getException(): Throwable;

        completeExceptionally(arg0: Throwable): void;

        complete(arg0: V): void;

        quietlyComplete(): void;

        get(): V;

        get(arg0: number, arg1: TimeUnit): V;

        quietlyJoin(): void;

        quietlyInvoke(): void;

        static helpQuiesce(): void;

        reinitialize(): void;

        static getPool(): ForkJoinPool;

        static inForkJoinPool(): boolean;

        tryUnfork(): boolean;

        static getQueuedTaskCount(): number;

        static getSurplusQueuedTaskCount(): number;

        abstract getRawResult(): V;

        getForkJoinTaskTag(): number;

        setForkJoinTaskTag(arg0: number): number;

        compareAndSetForkJoinTaskTag(arg0: number, arg1: number): boolean;

        static adapt(arg0: Runnable): ForkJoinTask<any>;

        static adapt<T extends Object>(arg0: Runnable, arg1: T): ForkJoinTask<T>;

        static adapt<T extends Object>(arg0: Callable<T>): ForkJoinTask<T>;
    }

    export class ForkJoinWorkerThread extends Thread {

        getPool(): ForkJoinPool;

        getPoolIndex(): number;

        run(): void;
    }

    export interface Future<V extends Object> extends Object {

        cancel(arg0: boolean): boolean;

        isCancelled(): boolean;

        isDone(): boolean;

        get(): V;

        get(arg0: number, arg1: TimeUnit): V;
    }

    export class FutureTask<V extends Object> extends Object implements RunnableFuture<V> {
        constructor(arg0: Callable<V>);
        constructor(arg0: Runnable, arg1: V);

        isCancelled(): boolean;

        isDone(): boolean;

        cancel(arg0: boolean): boolean;

        get(): V;

        get(arg0: number, arg1: TimeUnit): V;

        run(): void;
        toString(): string;
    }

    export interface LinkedBlockingDeque<E extends Object> extends BlockingDeque<E>, Serializable { }
    export class LinkedBlockingDeque<E extends Object> extends AbstractQueue<E> implements BlockingDeque<E>, Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Collection<E>);

        addFirst(arg0: E): void;

        addLast(arg0: E): void;

        offerFirst(arg0: E): boolean;

        offerLast(arg0: E): boolean;

        putFirst(arg0: E): void;

        putLast(arg0: E): void;

        offerFirst(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        offerLast(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        removeFirst(): E;

        removeLast(): E;

        pollFirst(): E;

        pollLast(): E;

        takeFirst(): E;

        takeLast(): E;

        pollFirst(arg0: number, arg1: TimeUnit): E;

        pollLast(arg0: number, arg1: TimeUnit): E;

        getFirst(): E;

        getLast(): E;

        peekFirst(): E;

        peekLast(): E;

        removeFirstOccurrence(arg0: Object): boolean;

        removeLastOccurrence(arg0: Object): boolean;

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        remove(): E;

        poll(): E;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        element(): E;

        peek(): E;

        remainingCapacity(): number;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        push(arg0: E): void;

        pop(): E;

        remove(arg0: Object): boolean;

        size(): number;

        contains(arg0: Object): boolean;

        addAll(arg0: Collection<E>): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];
        toString(): string;

        clear(): void;

        iterator(): Iterator<E>;

        descendingIterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        forEach(arg0: Consumer<E>): void;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;
    }

    export interface LinkedBlockingQueue<E extends Object> extends BlockingQueue<E>, Serializable { }
    export class LinkedBlockingQueue<E extends Object> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Collection<E>);

        size(): number;

        remainingCapacity(): number;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        offer(arg0: E): boolean;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        poll(): E;

        peek(): E;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];
        toString(): string;

        clear(): void;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        iterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        forEach(arg0: Consumer<E>): void;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;
    }

    export interface LinkedTransferQueue<E extends Object> extends TransferQueue<E>, Serializable { }
    export class LinkedTransferQueue<E extends Object> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {
        constructor();
        constructor(arg0: Collection<E>);
        toString(): string;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        spliterator(): Spliterator<E>;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        offer(arg0: E): boolean;

        add(arg0: E): boolean;

        tryTransfer(arg0: E): boolean;

        transfer(arg0: E): void;

        tryTransfer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        poll(): E;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        iterator(): Iterator<E>;

        peek(): E;

        isEmpty(): boolean;

        hasWaitingConsumer(): boolean;

        size(): number;

        getWaitingConsumerCount(): number;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;

        remainingCapacity(): number;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        clear(): void;

        forEach(arg0: Consumer<E>): void;
    }

    export class Phaser {
        constructor();
        constructor(arg0: number);
        constructor(arg0: Phaser);
        constructor(arg0: Phaser, arg1: number);

        register(): number;

        bulkRegister(arg0: number): number;

        arrive(): number;

        arriveAndDeregister(): number;

        arriveAndAwaitAdvance(): number;

        awaitAdvance(arg0: number): number;

        awaitAdvanceInterruptibly(arg0: number): number;

        awaitAdvanceInterruptibly(arg0: number, arg1: number, arg2: TimeUnit): number;

        forceTermination(): void;

        getPhase(): number;

        getRegisteredParties(): number;

        getArrivedParties(): number;

        getUnarrivedParties(): number;

        getParent(): Phaser;

        getRoot(): Phaser;

        isTerminated(): boolean;
        toString(): string;
    }

    export interface PriorityBlockingQueue<E extends Object> extends BlockingQueue<E>, Serializable { }
    export class PriorityBlockingQueue<E extends Object> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: number, arg1: Comparator<E>);
        constructor(arg0: Collection<E>);

        add(arg0: E): boolean;

        offer(arg0: E): boolean;

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        poll(): E;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        peek(): E;

        comparator(): Comparator<E>;

        size(): number;

        remainingCapacity(): number;

        remove(arg0: Object): boolean;

        contains(arg0: Object): boolean;
        toString(): string;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;

        clear(): void;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];

        iterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        removeIf(arg0: Predicate<E>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        forEach(arg0: Consumer<E>): void;
    }

    export abstract class RecursiveAction extends ForkJoinTask<Void> {
        constructor();

        getRawResult(): Void;
    }

    export abstract class RecursiveTask<V extends Object> extends ForkJoinTask<V> {
        constructor();

        getRawResult(): V;
    }

    export class RejectedExecutionException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export interface RejectedExecutionHandler {

        rejectedExecution(arg0: Runnable, arg1: ThreadPoolExecutor): void;
    }

    export interface RunnableFuture<V extends Object> extends Runnable, Future<V>, Object {

        run(): void;
    }

    export interface RunnableScheduledFuture<V extends Object> extends RunnableFuture<V>, ScheduledFuture<V>, Object {

        isPeriodic(): boolean;
    }

    export interface ScheduledExecutorService extends ExecutorService {

        schedule(arg0: Runnable, arg1: number, arg2: TimeUnit): ScheduledFuture<any>;

        schedule<V extends Object>(arg0: Callable<V>, arg1: number, arg2: TimeUnit): ScheduledFuture<V>;

        scheduleAtFixedRate(arg0: Runnable, arg1: number, arg2: number, arg3: TimeUnit): ScheduledFuture<any>;

        scheduleWithFixedDelay(arg0: Runnable, arg1: number, arg2: number, arg3: TimeUnit): ScheduledFuture<any>;
    }

    export interface ScheduledFuture<V extends Object> extends Delayed, Future<V>, Object {
    }

    export class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {
        constructor(arg0: number);
        constructor(arg0: number, arg1: ThreadFactory);
        constructor(arg0: number, arg1: RejectedExecutionHandler);
        constructor(arg0: number, arg1: ThreadFactory, arg2: RejectedExecutionHandler);

        schedule(arg0: Runnable, arg1: number, arg2: TimeUnit): ScheduledFuture<any>;

        schedule<V extends Object>(arg0: Callable<V>, arg1: number, arg2: TimeUnit): ScheduledFuture<V>;

        scheduleAtFixedRate(arg0: Runnable, arg1: number, arg2: number, arg3: TimeUnit): ScheduledFuture<any>;

        scheduleWithFixedDelay(arg0: Runnable, arg1: number, arg2: number, arg3: TimeUnit): ScheduledFuture<any>;

        execute(arg0: Runnable): void;

        submit(arg0: Runnable): Future<any>;

        submit<T extends Object>(arg0: Runnable, arg1: T): Future<T>;

        submit<T extends Object>(arg0: Callable<T>): Future<T>;

        setContinueExistingPeriodicTasksAfterShutdownPolicy(arg0: boolean): void;

        getContinueExistingPeriodicTasksAfterShutdownPolicy(): boolean;

        setExecuteExistingDelayedTasksAfterShutdownPolicy(arg0: boolean): void;

        getExecuteExistingDelayedTasksAfterShutdownPolicy(): boolean;

        setRemoveOnCancelPolicy(arg0: boolean): void;

        getRemoveOnCancelPolicy(): boolean;

        shutdown(): void;

        shutdownNow(): List<Runnable>;

        getQueue(): BlockingQueue<Runnable>;
    }

    export class Semaphore implements Serializable {
        constructor(arg0: number);
        constructor(arg0: number, arg1: boolean);

        acquire(): void;

        acquireUninterruptibly(): void;

        tryAcquire(): boolean;

        tryAcquire(arg0: number, arg1: TimeUnit): boolean;

        release(): void;

        acquire(arg0: number): void;

        acquireUninterruptibly(arg0: number): void;

        tryAcquire(arg0: number): boolean;

        tryAcquire(arg0: number, arg1: number, arg2: TimeUnit): boolean;

        release(arg0: number): void;

        availablePermits(): number;

        drainPermits(): number;

        isFair(): boolean;

        hasQueuedThreads(): boolean;

        getQueueLength(): number;
        toString(): string;
    }

    export class SubmissionPublisher<T extends Object> extends Object implements Flow.Publisher<T>, AutoCloseable {
        constructor(arg0: Executor, arg1: number, arg2: BiConsumer<Flow.Subscriber<T>, Throwable>);
        constructor(arg0: Executor, arg1: number);
        constructor();

        subscribe(arg0: Flow.Subscriber<T>): void;

        submit(arg0: T): number;

        offer(arg0: T, arg1: BiPredicate<Flow.Subscriber<T>, T>): number;

        offer(arg0: T, arg1: number, arg2: TimeUnit, arg3: BiPredicate<Flow.Subscriber<T>, T>): number;

        close(): void;

        closeExceptionally(arg0: Throwable): void;

        isClosed(): boolean;

        getClosedException(): Throwable;

        hasSubscribers(): boolean;

        getNumberOfSubscribers(): number;

        getExecutor(): Executor;

        getMaxBufferCapacity(): number;

        getSubscribers(): List<Flow.Subscriber<T>>;

        isSubscribed(arg0: Flow.Subscriber<T>): boolean;

        estimateMinimumDemand(): number;

        estimateMaximumLag(): number;

        consume(arg0: Consumer<T>): CompletableFuture<Void>;
    }

    export interface SynchronousQueue<E extends Object> extends BlockingQueue<E>, Serializable { }
    export class SynchronousQueue<E extends Object> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
        constructor();
        constructor(arg0: boolean);

        put(arg0: E): void;

        offer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        offer(arg0: E): boolean;

        take(): E;

        poll(arg0: number, arg1: TimeUnit): E;

        poll(): E;

        isEmpty(): boolean;

        size(): number;

        remainingCapacity(): number;

        clear(): void;

        contains(arg0: Object): boolean;

        remove(arg0: Object): boolean;

        containsAll(arg0: Collection<any>): boolean;

        removeAll(arg0: Collection<any>): boolean;

        retainAll(arg0: Collection<any>): boolean;

        peek(): E;

        iterator(): Iterator<E>;

        spliterator(): Spliterator<E>;

        toArray(): Object[];

        toArray<T extends Object>(arg0: T[]): T[];
        toString(): string;

        drainTo(arg0: Collection<E>): number;

        drainTo(arg0: Collection<E>, arg1: number): number;
    }

    export interface ThreadFactory {

        newThread(arg0: Runnable): Thread;
    }

    export interface ThreadLocalRandom { }
    export class ThreadLocalRandom extends Random {

        static current(): ThreadLocalRandom;

        setSeed(arg0: number): void;

        nextBoolean(): boolean;

        nextInt(): number;

        nextInt(arg0: number): number;

        nextInt(arg0: number, arg1: number): number;

        nextLong(): number;

        nextLong(arg0: number): number;

        nextLong(arg0: number, arg1: number): number;

        nextFloat(): number;

        nextFloat(arg0: number): number;

        nextFloat(arg0: number, arg1: number): number;

        nextDouble(): number;

        nextDouble(arg0: number): number;

        nextDouble(arg0: number, arg1: number): number;

        ints(arg0: number): IntStream;

        ints(): IntStream;

        ints(arg0: number, arg1: number, arg2: number): IntStream;

        ints(arg0: number, arg1: number): IntStream;

        longs(arg0: number): LongStream;

        longs(): LongStream;

        longs(arg0: number, arg1: number, arg2: number): LongStream;

        longs(arg0: number, arg1: number): LongStream;

        doubles(arg0: number): DoubleStream;

        doubles(): DoubleStream;

        doubles(arg0: number, arg1: number, arg2: number): DoubleStream;

        doubles(arg0: number, arg1: number): DoubleStream;
    }

    export class ThreadPoolExecutor extends AbstractExecutorService {
        constructor(arg0: number, arg1: number, arg2: number, arg3: TimeUnit, arg4: BlockingQueue<Runnable>);
        constructor(arg0: number, arg1: number, arg2: number, arg3: TimeUnit, arg4: BlockingQueue<Runnable>, arg5: ThreadFactory);
        constructor(arg0: number, arg1: number, arg2: number, arg3: TimeUnit, arg4: BlockingQueue<Runnable>, arg5: RejectedExecutionHandler);
        constructor(arg0: number, arg1: number, arg2: number, arg3: TimeUnit, arg4: BlockingQueue<Runnable>, arg5: ThreadFactory, arg6: RejectedExecutionHandler);

        execute(arg0: Runnable): void;

        shutdown(): void;

        shutdownNow(): List<Runnable>;

        isShutdown(): boolean;

        isTerminating(): boolean;

        isTerminated(): boolean;

        awaitTermination(arg0: number, arg1: TimeUnit): boolean;

        setThreadFactory(arg0: ThreadFactory): void;

        getThreadFactory(): ThreadFactory;

        setRejectedExecutionHandler(arg0: RejectedExecutionHandler): void;

        getRejectedExecutionHandler(): RejectedExecutionHandler;

        setCorePoolSize(arg0: number): void;

        getCorePoolSize(): number;

        prestartCoreThread(): boolean;

        prestartAllCoreThreads(): number;

        allowsCoreThreadTimeOut(): boolean;

        allowCoreThreadTimeOut(arg0: boolean): void;

        setMaximumPoolSize(arg0: number): void;

        getMaximumPoolSize(): number;

        setKeepAliveTime(arg0: number, arg1: TimeUnit): void;

        getKeepAliveTime(arg0: TimeUnit): number;

        getQueue(): BlockingQueue<Runnable>;

        remove(arg0: Runnable): boolean;

        purge(): void;

        getPoolSize(): number;

        getActiveCount(): number;

        getLargestPoolSize(): number;

        getTaskCount(): number;

        getCompletedTaskCount(): number;
        toString(): string;
    }
    export namespace ThreadPoolExecutor {
        export class AbortPolicy implements RejectedExecutionHandler {
            constructor();

            rejectedExecution(arg0: Runnable, arg1: ThreadPoolExecutor): void;
        }

        export class CallerRunsPolicy implements RejectedExecutionHandler {
            constructor();

            rejectedExecution(arg0: Runnable, arg1: ThreadPoolExecutor): void;
        }

        export class DiscardOldestPolicy implements RejectedExecutionHandler {
            constructor();

            rejectedExecution(arg0: Runnable, arg1: ThreadPoolExecutor): void;
        }

        export class DiscardPolicy implements RejectedExecutionHandler {
            constructor();

            rejectedExecution(arg0: Runnable, arg1: ThreadPoolExecutor): void;
        }

    }

    export class TimeUnit extends Enum<TimeUnit> {
        static NANOSECONDS: TimeUnit
        static MICROSECONDS: TimeUnit
        static MILLISECONDS: TimeUnit
        static SECONDS: TimeUnit
        static MINUTES: TimeUnit
        static HOURS: TimeUnit
        static DAYS: TimeUnit

        static values(): TimeUnit[];

        static valueOf(arg0: String): TimeUnit;

        convert(arg0: number, arg1: TimeUnit): number;

        convert(arg0: Duration): number;

        toNanos(arg0: number): number;

        toMicros(arg0: number): number;

        toMillis(arg0: number): number;

        toSeconds(arg0: number): number;

        toMinutes(arg0: number): number;

        toHours(arg0: number): number;

        toDays(arg0: number): number;

        timedWait(arg0: Object, arg1: number): void;

        timedJoin(arg0: Thread, arg1: number): void;

        sleep(arg0: number): void;

        toChronoUnit(): ChronoUnit;

        static of(arg0: ChronoUnit): TimeUnit;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class TimeoutException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface TransferQueue<E extends Object> extends BlockingQueue<E>, Object {

        tryTransfer(arg0: E): boolean;

        transfer(arg0: E): void;

        tryTransfer(arg0: E, arg1: number, arg2: TimeUnit): boolean;

        hasWaitingConsumer(): boolean;

        getWaitingConsumerCount(): number;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.util.concurrent.atomic' {
    import { Number, Class, String } from '@java/java.lang'
    import { Serializable } from '@java/java.io'
    import { DoubleBinaryOperator, LongBinaryOperator, IntBinaryOperator, LongUnaryOperator, IntUnaryOperator, UnaryOperator, BinaryOperator } from '@java/java.util.function'
    export class AtomicBoolean implements Serializable {
        constructor(arg0: boolean);
        constructor();

        get(): boolean;

        compareAndSet(arg0: boolean, arg1: boolean): boolean;

        weakCompareAndSet(arg0: boolean, arg1: boolean): boolean;

        weakCompareAndSetPlain(arg0: boolean, arg1: boolean): boolean;

        set(arg0: boolean): void;

        lazySet(arg0: boolean): void;

        getAndSet(arg0: boolean): boolean;
        toString(): string;

        getPlain(): boolean;

        setPlain(arg0: boolean): void;

        getOpaque(): boolean;

        setOpaque(arg0: boolean): void;

        getAcquire(): boolean;

        setRelease(arg0: boolean): void;

        compareAndExchange(arg0: boolean, arg1: boolean): boolean;

        compareAndExchangeAcquire(arg0: boolean, arg1: boolean): boolean;

        compareAndExchangeRelease(arg0: boolean, arg1: boolean): boolean;

        weakCompareAndSetVolatile(arg0: boolean, arg1: boolean): boolean;

        weakCompareAndSetAcquire(arg0: boolean, arg1: boolean): boolean;

        weakCompareAndSetRelease(arg0: boolean, arg1: boolean): boolean;
    }

    export class AtomicInteger extends Number implements Serializable {
        constructor(arg0: number);
        constructor();

        get(): number;

        set(arg0: number): void;

        lazySet(arg0: number): void;

        getAndSet(arg0: number): number;

        compareAndSet(arg0: number, arg1: number): boolean;

        weakCompareAndSet(arg0: number, arg1: number): boolean;

        weakCompareAndSetPlain(arg0: number, arg1: number): boolean;

        getAndIncrement(): number;

        getAndDecrement(): number;

        getAndAdd(arg0: number): number;

        incrementAndGet(): number;

        decrementAndGet(): number;

        addAndGet(arg0: number): number;

        getAndUpdate(arg0: IntUnaryOperator): number;

        updateAndGet(arg0: IntUnaryOperator): number;

        getAndAccumulate(arg0: number, arg1: IntBinaryOperator): number;

        accumulateAndGet(arg0: number, arg1: IntBinaryOperator): number;
        toString(): string;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;

        getPlain(): number;

        setPlain(arg0: number): void;

        getOpaque(): number;

        setOpaque(arg0: number): void;

        getAcquire(): number;

        setRelease(arg0: number): void;

        compareAndExchange(arg0: number, arg1: number): number;

        compareAndExchangeAcquire(arg0: number, arg1: number): number;

        compareAndExchangeRelease(arg0: number, arg1: number): number;

        weakCompareAndSetVolatile(arg0: number, arg1: number): boolean;

        weakCompareAndSetAcquire(arg0: number, arg1: number): boolean;

        weakCompareAndSetRelease(arg0: number, arg1: number): boolean;
    }

    export class AtomicIntegerArray implements Serializable {
        constructor(arg0: number);
        constructor(arg0: number[]);

        length(): number;

        get(arg0: number): number;

        set(arg0: number, arg1: number): void;

        lazySet(arg0: number, arg1: number): void;

        getAndSet(arg0: number, arg1: number): number;

        compareAndSet(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSet(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetPlain(arg0: number, arg1: number, arg2: number): boolean;

        getAndIncrement(arg0: number): number;

        getAndDecrement(arg0: number): number;

        getAndAdd(arg0: number, arg1: number): number;

        incrementAndGet(arg0: number): number;

        decrementAndGet(arg0: number): number;

        addAndGet(arg0: number, arg1: number): number;

        getAndUpdate(arg0: number, arg1: IntUnaryOperator): number;

        updateAndGet(arg0: number, arg1: IntUnaryOperator): number;

        getAndAccumulate(arg0: number, arg1: number, arg2: IntBinaryOperator): number;

        accumulateAndGet(arg0: number, arg1: number, arg2: IntBinaryOperator): number;
        toString(): string;

        getPlain(arg0: number): number;

        setPlain(arg0: number, arg1: number): void;

        getOpaque(arg0: number): number;

        setOpaque(arg0: number, arg1: number): void;

        getAcquire(arg0: number): number;

        setRelease(arg0: number, arg1: number): void;

        compareAndExchange(arg0: number, arg1: number, arg2: number): number;

        compareAndExchangeAcquire(arg0: number, arg1: number, arg2: number): number;

        compareAndExchangeRelease(arg0: number, arg1: number, arg2: number): number;

        weakCompareAndSetVolatile(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetAcquire(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetRelease(arg0: number, arg1: number, arg2: number): boolean;
    }

    export abstract class AtomicIntegerFieldUpdater<T extends Object> extends Object {

        static newUpdater<U extends Object>(arg0: Class<U>, arg1: String): AtomicIntegerFieldUpdater<U>;

        abstract compareAndSet(arg0: T, arg1: number, arg2: number): boolean;

        abstract weakCompareAndSet(arg0: T, arg1: number, arg2: number): boolean;

        abstract set(arg0: T, arg1: number): void;

        abstract lazySet(arg0: T, arg1: number): void;

        abstract get(arg0: T): number;

        getAndSet(arg0: T, arg1: number): number;

        getAndIncrement(arg0: T): number;

        getAndDecrement(arg0: T): number;

        getAndAdd(arg0: T, arg1: number): number;

        incrementAndGet(arg0: T): number;

        decrementAndGet(arg0: T): number;

        addAndGet(arg0: T, arg1: number): number;

        getAndUpdate(arg0: T, arg1: IntUnaryOperator): number;

        updateAndGet(arg0: T, arg1: IntUnaryOperator): number;

        getAndAccumulate(arg0: T, arg1: number, arg2: IntBinaryOperator): number;

        accumulateAndGet(arg0: T, arg1: number, arg2: IntBinaryOperator): number;
    }

    export class AtomicLong extends Number implements Serializable {
        constructor(arg0: number);
        constructor();

        get(): number;

        set(arg0: number): void;

        lazySet(arg0: number): void;

        getAndSet(arg0: number): number;

        compareAndSet(arg0: number, arg1: number): boolean;

        weakCompareAndSet(arg0: number, arg1: number): boolean;

        weakCompareAndSetPlain(arg0: number, arg1: number): boolean;

        getAndIncrement(): number;

        getAndDecrement(): number;

        getAndAdd(arg0: number): number;

        incrementAndGet(): number;

        decrementAndGet(): number;

        addAndGet(arg0: number): number;

        getAndUpdate(arg0: LongUnaryOperator): number;

        updateAndGet(arg0: LongUnaryOperator): number;

        getAndAccumulate(arg0: number, arg1: LongBinaryOperator): number;

        accumulateAndGet(arg0: number, arg1: LongBinaryOperator): number;
        toString(): string;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;

        getPlain(): number;

        setPlain(arg0: number): void;

        getOpaque(): number;

        setOpaque(arg0: number): void;

        getAcquire(): number;

        setRelease(arg0: number): void;

        compareAndExchange(arg0: number, arg1: number): number;

        compareAndExchangeAcquire(arg0: number, arg1: number): number;

        compareAndExchangeRelease(arg0: number, arg1: number): number;

        weakCompareAndSetVolatile(arg0: number, arg1: number): boolean;

        weakCompareAndSetAcquire(arg0: number, arg1: number): boolean;

        weakCompareAndSetRelease(arg0: number, arg1: number): boolean;
    }

    export class AtomicLongArray implements Serializable {
        constructor(arg0: number);
        constructor(arg0: number[]);

        length(): number;

        get(arg0: number): number;

        set(arg0: number, arg1: number): void;

        lazySet(arg0: number, arg1: number): void;

        getAndSet(arg0: number, arg1: number): number;

        compareAndSet(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSet(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetPlain(arg0: number, arg1: number, arg2: number): boolean;

        getAndIncrement(arg0: number): number;

        getAndDecrement(arg0: number): number;

        getAndAdd(arg0: number, arg1: number): number;

        incrementAndGet(arg0: number): number;

        decrementAndGet(arg0: number): number;

        addAndGet(arg0: number, arg1: number): number;

        getAndUpdate(arg0: number, arg1: LongUnaryOperator): number;

        updateAndGet(arg0: number, arg1: LongUnaryOperator): number;

        getAndAccumulate(arg0: number, arg1: number, arg2: LongBinaryOperator): number;

        accumulateAndGet(arg0: number, arg1: number, arg2: LongBinaryOperator): number;
        toString(): string;

        getPlain(arg0: number): number;

        setPlain(arg0: number, arg1: number): void;

        getOpaque(arg0: number): number;

        setOpaque(arg0: number, arg1: number): void;

        getAcquire(arg0: number): number;

        setRelease(arg0: number, arg1: number): void;

        compareAndExchange(arg0: number, arg1: number, arg2: number): number;

        compareAndExchangeAcquire(arg0: number, arg1: number, arg2: number): number;

        compareAndExchangeRelease(arg0: number, arg1: number, arg2: number): number;

        weakCompareAndSetVolatile(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetAcquire(arg0: number, arg1: number, arg2: number): boolean;

        weakCompareAndSetRelease(arg0: number, arg1: number, arg2: number): boolean;
    }

    export abstract class AtomicLongFieldUpdater<T extends Object> extends Object {

        static newUpdater<U extends Object>(arg0: Class<U>, arg1: String): AtomicLongFieldUpdater<U>;

        abstract compareAndSet(arg0: T, arg1: number, arg2: number): boolean;

        abstract weakCompareAndSet(arg0: T, arg1: number, arg2: number): boolean;

        abstract set(arg0: T, arg1: number): void;

        abstract lazySet(arg0: T, arg1: number): void;

        abstract get(arg0: T): number;

        getAndSet(arg0: T, arg1: number): number;

        getAndIncrement(arg0: T): number;

        getAndDecrement(arg0: T): number;

        getAndAdd(arg0: T, arg1: number): number;

        incrementAndGet(arg0: T): number;

        decrementAndGet(arg0: T): number;

        addAndGet(arg0: T, arg1: number): number;

        getAndUpdate(arg0: T, arg1: LongUnaryOperator): number;

        updateAndGet(arg0: T, arg1: LongUnaryOperator): number;

        getAndAccumulate(arg0: T, arg1: number, arg2: LongBinaryOperator): number;

        accumulateAndGet(arg0: T, arg1: number, arg2: LongBinaryOperator): number;
    }

    export class AtomicMarkableReference<V extends Object> extends Object {
        constructor(arg0: V, arg1: boolean);

        getReference(): V;

        isMarked(): boolean;

        get(arg0: boolean[]): V;

        weakCompareAndSet(arg0: V, arg1: V, arg2: boolean, arg3: boolean): boolean;

        compareAndSet(arg0: V, arg1: V, arg2: boolean, arg3: boolean): boolean;

        set(arg0: V, arg1: boolean): void;

        attemptMark(arg0: V, arg1: boolean): boolean;
    }

    export class AtomicReference<V extends Object> extends Object implements Serializable {
        constructor(arg0: V);
        constructor();

        get(): V;

        set(arg0: V): void;

        lazySet(arg0: V): void;

        compareAndSet(arg0: V, arg1: V): boolean;

        weakCompareAndSet(arg0: V, arg1: V): boolean;

        weakCompareAndSetPlain(arg0: V, arg1: V): boolean;

        getAndSet(arg0: V): V;

        getAndUpdate(arg0: UnaryOperator<V>): V;

        updateAndGet(arg0: UnaryOperator<V>): V;

        getAndAccumulate(arg0: V, arg1: BinaryOperator<V>): V;

        accumulateAndGet(arg0: V, arg1: BinaryOperator<V>): V;
        toString(): string;

        getPlain(): V;

        setPlain(arg0: V): void;

        getOpaque(): V;

        setOpaque(arg0: V): void;

        getAcquire(): V;

        setRelease(arg0: V): void;

        compareAndExchange(arg0: V, arg1: V): V;

        compareAndExchangeAcquire(arg0: V, arg1: V): V;

        compareAndExchangeRelease(arg0: V, arg1: V): V;

        weakCompareAndSetVolatile(arg0: V, arg1: V): boolean;

        weakCompareAndSetAcquire(arg0: V, arg1: V): boolean;

        weakCompareAndSetRelease(arg0: V, arg1: V): boolean;
    }

    export class AtomicReferenceArray<E extends Object> extends Object implements Serializable {
        constructor(arg0: number);
        constructor(arg0: E[]);

        length(): number;

        get(arg0: number): E;

        set(arg0: number, arg1: E): void;

        lazySet(arg0: number, arg1: E): void;

        getAndSet(arg0: number, arg1: E): E;

        compareAndSet(arg0: number, arg1: E, arg2: E): boolean;

        weakCompareAndSet(arg0: number, arg1: E, arg2: E): boolean;

        weakCompareAndSetPlain(arg0: number, arg1: E, arg2: E): boolean;

        getAndUpdate(arg0: number, arg1: UnaryOperator<E>): E;

        updateAndGet(arg0: number, arg1: UnaryOperator<E>): E;

        getAndAccumulate(arg0: number, arg1: E, arg2: BinaryOperator<E>): E;

        accumulateAndGet(arg0: number, arg1: E, arg2: BinaryOperator<E>): E;
        toString(): string;

        getPlain(arg0: number): E;

        setPlain(arg0: number, arg1: E): void;

        getOpaque(arg0: number): E;

        setOpaque(arg0: number, arg1: E): void;

        getAcquire(arg0: number): E;

        setRelease(arg0: number, arg1: E): void;

        compareAndExchange(arg0: number, arg1: E, arg2: E): E;

        compareAndExchangeAcquire(arg0: number, arg1: E, arg2: E): E;

        compareAndExchangeRelease(arg0: number, arg1: E, arg2: E): E;

        weakCompareAndSetVolatile(arg0: number, arg1: E, arg2: E): boolean;

        weakCompareAndSetAcquire(arg0: number, arg1: E, arg2: E): boolean;

        weakCompareAndSetRelease(arg0: number, arg1: E, arg2: E): boolean;
    }

    export abstract class AtomicReferenceFieldUpdater<T extends Object, V extends Object> extends Object {

        static newUpdater<U extends Object, W extends Object>(arg0: Class<U>, arg1: Class<W>, arg2: String): AtomicReferenceFieldUpdater<U, W>;

        abstract compareAndSet(arg0: T, arg1: V, arg2: V): boolean;

        abstract weakCompareAndSet(arg0: T, arg1: V, arg2: V): boolean;

        abstract set(arg0: T, arg1: V): void;

        abstract lazySet(arg0: T, arg1: V): void;

        abstract get(arg0: T): V;

        getAndSet(arg0: T, arg1: V): V;

        getAndUpdate(arg0: T, arg1: UnaryOperator<V>): V;

        updateAndGet(arg0: T, arg1: UnaryOperator<V>): V;

        getAndAccumulate(arg0: T, arg1: V, arg2: BinaryOperator<V>): V;

        accumulateAndGet(arg0: T, arg1: V, arg2: BinaryOperator<V>): V;
    }

    export class AtomicStampedReference<V extends Object> extends Object {
        constructor(arg0: V, arg1: number);

        getReference(): V;

        getStamp(): number;

        get(arg0: number[]): V;

        weakCompareAndSet(arg0: V, arg1: V, arg2: number, arg3: number): boolean;

        compareAndSet(arg0: V, arg1: V, arg2: number, arg3: number): boolean;

        set(arg0: V, arg1: number): void;

        attemptStamp(arg0: V, arg1: number): boolean;
    }

    export class DoubleAccumulator extends Striped64 implements Serializable {
        constructor(arg0: DoubleBinaryOperator, arg1: number);

        accumulate(arg0: number): void;

        get(): number;

        reset(): void;

        getThenReset(): number;
        toString(): string;

        doubleValue(): number;

        longValue(): number;

        intValue(): number;

        floatValue(): number;
    }

    export class DoubleAdder extends Striped64 implements Serializable {
        constructor();

        add(arg0: number): void;

        sum(): number;

        reset(): void;

        sumThenReset(): number;
        toString(): string;

        doubleValue(): number;

        longValue(): number;

        intValue(): number;

        floatValue(): number;
    }

    export class LongAccumulator extends Striped64 implements Serializable {
        constructor(arg0: LongBinaryOperator, arg1: number);

        accumulate(arg0: number): void;

        get(): number;

        reset(): void;

        getThenReset(): number;
        toString(): string;

        longValue(): number;

        intValue(): number;

        floatValue(): number;

        doubleValue(): number;
    }

    export class LongAdder extends Striped64 implements Serializable {
        constructor();

        add(arg0: number): void;

        increment(): void;

        decrement(): void;

        sum(): number;

        reset(): void;

        sumThenReset(): number;
        toString(): string;

        longValue(): number;

        intValue(): number;

        floatValue(): number;

        doubleValue(): number;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.time.zone' {
    import { Enum, Throwable, Comparable, Class, String } from '@java/java.lang'
    import { LocalDateTime, Month, DayOfWeek, DateTimeException, LocalTime, Instant, Duration, ZoneOffset } from '@java/java.time'
    import { NavigableMap, List, Set } from '@java/java.util'
    import { Serializable } from '@java/java.io'
    export class ZoneOffsetTransition extends Object implements Comparable<ZoneOffsetTransition>, Serializable {

        static of(arg0: LocalDateTime, arg1: ZoneOffset, arg2: ZoneOffset): ZoneOffsetTransition;

        getInstant(): Instant;

        toEpochSecond(): number;

        getDateTimeBefore(): LocalDateTime;

        getDateTimeAfter(): LocalDateTime;

        getOffsetBefore(): ZoneOffset;

        getOffsetAfter(): ZoneOffset;

        getDuration(): Duration;

        isGap(): boolean;

        isOverlap(): boolean;

        isValidOffset(arg0: ZoneOffset): boolean;

        compareTo(arg0: ZoneOffsetTransition): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class ZoneOffsetTransitionRule implements Serializable {

        static of(arg0: Month, arg1: number, arg2: DayOfWeek, arg3: LocalTime, arg4: boolean, arg5: ZoneOffsetTransitionRule.TimeDefinition, arg6: ZoneOffset, arg7: ZoneOffset, arg8: ZoneOffset): ZoneOffsetTransitionRule;

        getMonth(): Month;

        getDayOfMonthIndicator(): number;

        getDayOfWeek(): DayOfWeek;

        getLocalTime(): LocalTime;

        isMidnightEndOfDay(): boolean;

        getTimeDefinition(): ZoneOffsetTransitionRule.TimeDefinition;

        getStandardOffset(): ZoneOffset;

        getOffsetBefore(): ZoneOffset;

        getOffsetAfter(): ZoneOffset;

        createTransition(arg0: number): ZoneOffsetTransition;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }
    export namespace ZoneOffsetTransitionRule {
        export class TimeDefinition extends Enum<ZoneOffsetTransitionRule.TimeDefinition> {
            static UTC: ZoneOffsetTransitionRule.TimeDefinition
            static WALL: ZoneOffsetTransitionRule.TimeDefinition
            static STANDARD: ZoneOffsetTransitionRule.TimeDefinition

            static values(): ZoneOffsetTransitionRule.TimeDefinition[];

            static valueOf(arg0: String): ZoneOffsetTransitionRule.TimeDefinition;

            createDateTime(arg0: LocalDateTime, arg1: ZoneOffset, arg2: ZoneOffset): LocalDateTime;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class ZoneRules implements Serializable {

        static of(arg0: ZoneOffset, arg1: ZoneOffset, arg2: List<ZoneOffsetTransition>, arg3: List<ZoneOffsetTransition>, arg4: List<ZoneOffsetTransitionRule>): ZoneRules;

        static of(arg0: ZoneOffset): ZoneRules;

        isFixedOffset(): boolean;

        getOffset(arg0: Instant): ZoneOffset;

        getOffset(arg0: LocalDateTime): ZoneOffset;

        getValidOffsets(arg0: LocalDateTime): List<ZoneOffset>;

        getTransition(arg0: LocalDateTime): ZoneOffsetTransition;

        getStandardOffset(arg0: Instant): ZoneOffset;

        getDaylightSavings(arg0: Instant): Duration;

        isDaylightSavings(arg0: Instant): boolean;

        isValidOffset(arg0: LocalDateTime, arg1: ZoneOffset): boolean;

        nextTransition(arg0: Instant): ZoneOffsetTransition;

        previousTransition(arg0: Instant): ZoneOffsetTransition;

        getTransitions(): List<ZoneOffsetTransition>;

        getTransitionRules(): List<ZoneOffsetTransitionRule>;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class ZoneRulesException extends DateTimeException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export abstract class ZoneRulesProvider {

        static getAvailableZoneIds(): Set<String>;

        static getRules(arg0: String, arg1: boolean): ZoneRules;

        static getVersions(arg0: String): NavigableMap<String, ZoneRules>;

        static registerProvider(arg0: ZoneRulesProvider): void;

        static refresh(): boolean;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.time.format.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.time.chrono.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.time.temporal' {
    import { Enum, Long, Throwable, Class, String } from '@java/java.lang'
    import { Locale, Map, List } from '@java/java.util'
    import { DayOfWeek, DateTimeException, ZoneId, LocalTime, Duration, LocalDate, ZoneOffset } from '@java/java.time'
    import { ResolverStyle } from '@java/java.time.format'
    import { Serializable } from '@java/java.io'
    import { Chronology } from '@java/java.time.chrono'
    import { UnaryOperator } from '@java/java.util.function'
    export interface ChronoField extends TemporalField { }
    export class ChronoField extends Enum<ChronoField> implements TemporalField {
        static NANO_OF_SECOND: ChronoField
        static NANO_OF_DAY: ChronoField
        static MICRO_OF_SECOND: ChronoField
        static MICRO_OF_DAY: ChronoField
        static MILLI_OF_SECOND: ChronoField
        static MILLI_OF_DAY: ChronoField
        static SECOND_OF_MINUTE: ChronoField
        static SECOND_OF_DAY: ChronoField
        static MINUTE_OF_HOUR: ChronoField
        static MINUTE_OF_DAY: ChronoField
        static HOUR_OF_AMPM: ChronoField
        static CLOCK_HOUR_OF_AMPM: ChronoField
        static HOUR_OF_DAY: ChronoField
        static CLOCK_HOUR_OF_DAY: ChronoField
        static AMPM_OF_DAY: ChronoField
        static DAY_OF_WEEK: ChronoField
        static ALIGNED_DAY_OF_WEEK_IN_MONTH: ChronoField
        static ALIGNED_DAY_OF_WEEK_IN_YEAR: ChronoField
        static DAY_OF_MONTH: ChronoField
        static DAY_OF_YEAR: ChronoField
        static EPOCH_DAY: ChronoField
        static ALIGNED_WEEK_OF_MONTH: ChronoField
        static ALIGNED_WEEK_OF_YEAR: ChronoField
        static MONTH_OF_YEAR: ChronoField
        static PROLEPTIC_MONTH: ChronoField
        static YEAR_OF_ERA: ChronoField
        static YEAR: ChronoField
        static ERA: ChronoField
        static INSTANT_SECONDS: ChronoField
        static OFFSET_SECONDS: ChronoField

        static values(): ChronoField[];

        static valueOf(arg0: String): ChronoField;

        getDisplayName(arg0: Locale): String;

        getBaseUnit(): TemporalUnit;

        getRangeUnit(): TemporalUnit;

        range(): ValueRange;

        isDateBased(): boolean;

        isTimeBased(): boolean;

        checkValidValue(arg0: number): number;

        checkValidIntValue(arg0: number): number;

        isSupportedBy(arg0: TemporalAccessor): boolean;

        rangeRefinedBy(arg0: TemporalAccessor): ValueRange;

        getFrom(arg0: TemporalAccessor): number;

        adjustInto<R extends Temporal>(arg0: R, arg1: number): R;
        toString(): string;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class ChronoUnit extends Enum<ChronoUnit> implements TemporalUnit {
        static NANOS: ChronoUnit
        static MICROS: ChronoUnit
        static MILLIS: ChronoUnit
        static SECONDS: ChronoUnit
        static MINUTES: ChronoUnit
        static HOURS: ChronoUnit
        static HALF_DAYS: ChronoUnit
        static DAYS: ChronoUnit
        static WEEKS: ChronoUnit
        static MONTHS: ChronoUnit
        static YEARS: ChronoUnit
        static DECADES: ChronoUnit
        static CENTURIES: ChronoUnit
        static MILLENNIA: ChronoUnit
        static ERAS: ChronoUnit
        static FOREVER: ChronoUnit

        static values(): ChronoUnit[];

        static valueOf(arg0: String): ChronoUnit;

        getDuration(): Duration;

        isDurationEstimated(): boolean;

        isDateBased(): boolean;

        isTimeBased(): boolean;

        isSupportedBy(arg0: Temporal): boolean;

        addTo<R extends Temporal>(arg0: R, arg1: number): R;

        between(arg0: Temporal, arg1: Temporal): number;
        toString(): string;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class IsoFields {
        static DAY_OF_QUARTER: TemporalField
        static QUARTER_OF_YEAR: TemporalField
        static WEEK_OF_WEEK_BASED_YEAR: TemporalField
        static WEEK_BASED_YEAR: TemporalField
        static WEEK_BASED_YEARS: TemporalUnit
        static QUARTER_YEARS: TemporalUnit
    }

    export class JulianFields {
        static JULIAN_DAY: TemporalField
        static MODIFIED_JULIAN_DAY: TemporalField
        static RATA_DIE: TemporalField
    }

    export interface Temporal extends TemporalAccessor {

        isSupported(arg0: TemporalUnit): boolean;

/* default */ with(arg0: TemporalAdjuster): Temporal;

        with(arg0: TemporalField, arg1: number): Temporal;

/* default */ plus(arg0: TemporalAmount): Temporal;

        plus(arg0: number, arg1: TemporalUnit): Temporal;

/* default */ minus(arg0: TemporalAmount): Temporal;

/* default */ minus(arg0: number, arg1: TemporalUnit): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;
    }

    export interface TemporalAccessor {

        isSupported(arg0: TemporalField): boolean;

/* default */ range(arg0: TemporalField): ValueRange;

/* default */ get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

/* default */ query<R extends Object>(arg0: TemporalQuery<R>): R;
    }

    export interface TemporalAdjuster {

        adjustInto(arg0: Temporal): Temporal;
    }

    export class TemporalAdjusters {

        static ofDateAdjuster(arg0: UnaryOperator<LocalDate>): TemporalAdjuster;

        static firstDayOfMonth(): TemporalAdjuster;

        static lastDayOfMonth(): TemporalAdjuster;

        static firstDayOfNextMonth(): TemporalAdjuster;

        static firstDayOfYear(): TemporalAdjuster;

        static lastDayOfYear(): TemporalAdjuster;

        static firstDayOfNextYear(): TemporalAdjuster;

        static firstInMonth(arg0: DayOfWeek): TemporalAdjuster;

        static lastInMonth(arg0: DayOfWeek): TemporalAdjuster;

        static dayOfWeekInMonth(arg0: number, arg1: DayOfWeek): TemporalAdjuster;

        static next(arg0: DayOfWeek): TemporalAdjuster;

        static nextOrSame(arg0: DayOfWeek): TemporalAdjuster;

        static previous(arg0: DayOfWeek): TemporalAdjuster;

        static previousOrSame(arg0: DayOfWeek): TemporalAdjuster;
    }

    export interface TemporalAmount {

        get(arg0: TemporalUnit): number;

        getUnits(): List<TemporalUnit>;

        addTo(arg0: Temporal): Temporal;

        subtractFrom(arg0: Temporal): Temporal;
    }

    export interface TemporalField {

/* default */ getDisplayName(arg0: Locale): String;

        getBaseUnit(): TemporalUnit;

        getRangeUnit(): TemporalUnit;

        range(): ValueRange;

        isDateBased(): boolean;

        isTimeBased(): boolean;

        isSupportedBy(arg0: TemporalAccessor): boolean;

        rangeRefinedBy(arg0: TemporalAccessor): ValueRange;

        getFrom(arg0: TemporalAccessor): number;

        adjustInto<R extends Temporal>(arg0: R, arg1: number): R;

/* default */ resolve(arg0: Map<TemporalField, Number>, arg1: TemporalAccessor, arg2: ResolverStyle): TemporalAccessor;
        toString(): string;
    }

    export class TemporalQueries {

        static zoneId(): TemporalQuery<ZoneId>;

        static chronology(): TemporalQuery<Chronology>;

        static precision(): TemporalQuery<TemporalUnit>;

        static zone(): TemporalQuery<ZoneId>;

        static offset(): TemporalQuery<ZoneOffset>;

        static localDate(): TemporalQuery<LocalDate>;

        static localTime(): TemporalQuery<LocalTime>;
    }

    export interface TemporalQuery<R extends Object> extends Object {

        queryFrom(arg0: TemporalAccessor): R;
    }

    export interface TemporalUnit {

        getDuration(): Duration;

        isDurationEstimated(): boolean;

        isDateBased(): boolean;

        isTimeBased(): boolean;

/* default */ isSupportedBy(arg0: Temporal): boolean;

        addTo<R extends Temporal>(arg0: R, arg1: number): R;

        between(arg0: Temporal, arg1: Temporal): number;
        toString(): string;
    }

    export class UnsupportedTemporalTypeException extends DateTimeException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export class ValueRange implements Serializable {

        static of(arg0: number, arg1: number): ValueRange;

        static of(arg0: number, arg1: number, arg2: number): ValueRange;

        static of(arg0: number, arg1: number, arg2: number, arg3: number): ValueRange;

        isFixed(): boolean;

        getMinimum(): number;

        getLargestMinimum(): number;

        getSmallestMaximum(): number;

        getMaximum(): number;

        isIntValue(): boolean;

        isValidValue(arg0: number): boolean;

        isValidIntValue(arg0: number): boolean;

        checkValidValue(arg0: number, arg1: TemporalField): number;

        checkValidIntValue(arg0: number, arg1: TemporalField): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class WeekFields implements Serializable {
        static ISO: WeekFields
        static SUNDAY_START: WeekFields
        static WEEK_BASED_YEARS: TemporalUnit

        static of(arg0: Locale): WeekFields;

        static of(arg0: DayOfWeek, arg1: number): WeekFields;

        getFirstDayOfWeek(): DayOfWeek;

        getMinimalDaysInFirstWeek(): number;

        dayOfWeek(): TemporalField;

        weekOfMonth(): TemporalField;

        weekOfYear(): TemporalField;

        weekOfWeekBasedYear(): TemporalField;

        weekBasedYear(): TemporalField;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.text.d.ts" />
/// <reference path="java.time.chrono.d.ts" />
/// <reference path="java.time.temporal.d.ts" />
declare module '@java/java.time.format' {
    import { Enum, Appendable, CharSequence, Long, Throwable, Class, String, Boolean } from '@java/java.lang'
    import { Locale, Map, Set } from '@java/java.util'
    import { ZoneId, Period, DateTimeException } from '@java/java.time'
    import { ParsePosition, Format } from '@java/java.text'
    import { ChronoLocalDate, Chronology } from '@java/java.time.chrono'
    import { TemporalQuery, TemporalField, TemporalAccessor } from '@java/java.time.temporal'
    export class DateTimeFormatter {
        static ISO_LOCAL_DATE: DateTimeFormatter
        static ISO_OFFSET_DATE: DateTimeFormatter
        static ISO_DATE: DateTimeFormatter
        static ISO_LOCAL_TIME: DateTimeFormatter
        static ISO_OFFSET_TIME: DateTimeFormatter
        static ISO_TIME: DateTimeFormatter
        static ISO_LOCAL_DATE_TIME: DateTimeFormatter
        static ISO_OFFSET_DATE_TIME: DateTimeFormatter
        static ISO_ZONED_DATE_TIME: DateTimeFormatter
        static ISO_DATE_TIME: DateTimeFormatter
        static ISO_ORDINAL_DATE: DateTimeFormatter
        static ISO_WEEK_DATE: DateTimeFormatter
        static ISO_INSTANT: DateTimeFormatter
        static BASIC_ISO_DATE: DateTimeFormatter
        static RFC_1123_DATE_TIME: DateTimeFormatter

        static ofPattern(arg0: String): DateTimeFormatter;

        static ofPattern(arg0: String, arg1: Locale): DateTimeFormatter;

        static ofLocalizedDate(arg0: FormatStyle): DateTimeFormatter;

        static ofLocalizedTime(arg0: FormatStyle): DateTimeFormatter;

        static ofLocalizedDateTime(arg0: FormatStyle): DateTimeFormatter;

        static ofLocalizedDateTime(arg0: FormatStyle, arg1: FormatStyle): DateTimeFormatter;

        static parsedExcessDays(): TemporalQuery<Period>;

        static parsedLeapSecond(): TemporalQuery<Boolean>;

        getLocale(): Locale;

        withLocale(arg0: Locale): DateTimeFormatter;

        localizedBy(arg0: Locale): DateTimeFormatter;

        getDecimalStyle(): DecimalStyle;

        withDecimalStyle(arg0: DecimalStyle): DateTimeFormatter;

        getChronology(): Chronology;

        withChronology(arg0: Chronology): DateTimeFormatter;

        getZone(): ZoneId;

        withZone(arg0: ZoneId): DateTimeFormatter;

        getResolverStyle(): ResolverStyle;

        withResolverStyle(arg0: ResolverStyle): DateTimeFormatter;

        getResolverFields(): Set<TemporalField>;

        withResolverFields(arg0: TemporalField[]): DateTimeFormatter;

        withResolverFields(arg0: Set<TemporalField>): DateTimeFormatter;

        format(arg0: TemporalAccessor): String;

        formatTo(arg0: TemporalAccessor, arg1: Appendable): void;

        parse(arg0: CharSequence): TemporalAccessor;

        parse(arg0: CharSequence, arg1: ParsePosition): TemporalAccessor;

        parse<T extends Object>(arg0: CharSequence, arg1: TemporalQuery<T>): T;

        parseBest(arg0: CharSequence, arg1: TemporalQuery<any>[]): TemporalAccessor;

        parseUnresolved(arg0: CharSequence, arg1: ParsePosition): TemporalAccessor;

        toFormat(): Format;

        toFormat(arg0: TemporalQuery<any>): Format;
        toString(): string;
    }

    export class DateTimeFormatterBuilder {
        constructor();

        static getLocalizedDateTimePattern(arg0: FormatStyle, arg1: FormatStyle, arg2: Chronology, arg3: Locale): String;

        parseCaseSensitive(): DateTimeFormatterBuilder;

        parseCaseInsensitive(): DateTimeFormatterBuilder;

        parseStrict(): DateTimeFormatterBuilder;

        parseLenient(): DateTimeFormatterBuilder;

        parseDefaulting(arg0: TemporalField, arg1: number): DateTimeFormatterBuilder;

        appendValue(arg0: TemporalField): DateTimeFormatterBuilder;

        appendValue(arg0: TemporalField, arg1: number): DateTimeFormatterBuilder;

        appendValue(arg0: TemporalField, arg1: number, arg2: number, arg3: SignStyle): DateTimeFormatterBuilder;

        appendValueReduced(arg0: TemporalField, arg1: number, arg2: number, arg3: number): DateTimeFormatterBuilder;

        appendValueReduced(arg0: TemporalField, arg1: number, arg2: number, arg3: ChronoLocalDate): DateTimeFormatterBuilder;

        appendFraction(arg0: TemporalField, arg1: number, arg2: number, arg3: boolean): DateTimeFormatterBuilder;

        appendText(arg0: TemporalField): DateTimeFormatterBuilder;

        appendText(arg0: TemporalField, arg1: TextStyle): DateTimeFormatterBuilder;

        appendText(arg0: TemporalField, arg1: Map<Number, String>): DateTimeFormatterBuilder;

        appendInstant(): DateTimeFormatterBuilder;

        appendInstant(arg0: number): DateTimeFormatterBuilder;

        appendOffsetId(): DateTimeFormatterBuilder;

        appendOffset(arg0: String, arg1: String): DateTimeFormatterBuilder;

        appendLocalizedOffset(arg0: TextStyle): DateTimeFormatterBuilder;

        appendZoneId(): DateTimeFormatterBuilder;

        appendZoneRegionId(): DateTimeFormatterBuilder;

        appendZoneOrOffsetId(): DateTimeFormatterBuilder;

        appendZoneText(arg0: TextStyle): DateTimeFormatterBuilder;

        appendZoneText(arg0: TextStyle, arg1: Set<ZoneId>): DateTimeFormatterBuilder;

        appendGenericZoneText(arg0: TextStyle): DateTimeFormatterBuilder;

        appendGenericZoneText(arg0: TextStyle, arg1: Set<ZoneId>): DateTimeFormatterBuilder;

        appendChronologyId(): DateTimeFormatterBuilder;

        appendChronologyText(arg0: TextStyle): DateTimeFormatterBuilder;

        appendLocalized(arg0: FormatStyle, arg1: FormatStyle): DateTimeFormatterBuilder;

        appendLiteral(arg0: String): DateTimeFormatterBuilder;

        appendLiteral(arg0: String): DateTimeFormatterBuilder;

        appendDayPeriodText(arg0: TextStyle): DateTimeFormatterBuilder;

        append(arg0: DateTimeFormatter): DateTimeFormatterBuilder;

        appendOptional(arg0: DateTimeFormatter): DateTimeFormatterBuilder;

        appendPattern(arg0: String): DateTimeFormatterBuilder;

        padNext(arg0: number): DateTimeFormatterBuilder;

        padNext(arg0: number, arg1: String): DateTimeFormatterBuilder;

        optionalStart(): DateTimeFormatterBuilder;

        optionalEnd(): DateTimeFormatterBuilder;

        toFormatter(): DateTimeFormatter;

        toFormatter(arg0: Locale): DateTimeFormatter;
    }

    export class DateTimeParseException extends DateTimeException {
        constructor(arg0: String, arg1: CharSequence, arg2: number);
        constructor(arg0: String, arg1: CharSequence, arg2: number, arg3: Throwable);

        getParsedString(): String;

        getErrorIndex(): number;
    }

    export class DecimalStyle {
        static STANDARD: DecimalStyle

        static getAvailableLocales(): Set<Locale>;

        static ofDefaultLocale(): DecimalStyle;

        static of(arg0: Locale): DecimalStyle;

        getZeroDigit(): String;

        withZeroDigit(arg0: String): DecimalStyle;

        getPositiveSign(): String;

        withPositiveSign(arg0: String): DecimalStyle;

        getNegativeSign(): String;

        withNegativeSign(arg0: String): DecimalStyle;

        getDecimalSeparator(): String;

        withDecimalSeparator(arg0: String): DecimalStyle;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class FormatStyle extends Enum<FormatStyle> {
        static FULL: FormatStyle
        static LONG: FormatStyle
        static MEDIUM: FormatStyle
        static SHORT: FormatStyle

        static values(): FormatStyle[];

        static valueOf(arg0: String): FormatStyle;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class ResolverStyle extends Enum<ResolverStyle> {
        static STRICT: ResolverStyle
        static SMART: ResolverStyle
        static LENIENT: ResolverStyle

        static values(): ResolverStyle[];

        static valueOf(arg0: String): ResolverStyle;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class SignStyle extends Enum<SignStyle> {
        static NORMAL: SignStyle
        static ALWAYS: SignStyle
        static NEVER: SignStyle
        static NOT_NEGATIVE: SignStyle
        static EXCEEDS_PAD: SignStyle

        static values(): SignStyle[];

        static valueOf(arg0: String): SignStyle;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class TextStyle extends Enum<TextStyle> {
        static FULL: TextStyle
        static FULL_STANDALONE: TextStyle
        static SHORT: TextStyle
        static SHORT_STANDALONE: TextStyle
        static NARROW: TextStyle
        static NARROW_STANDALONE: TextStyle

        static values(): TextStyle[];

        static valueOf(arg0: String): TextStyle;

        isStandalone(): boolean;

        asStandalone(): TextStyle;

        asNormal(): TextStyle;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.format.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.time.chrono.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.time.zone.d.ts" />
/// <reference path="java.time.temporal.d.ts" />
declare module '@java/java.time' {
    import { Enum, Comparable, RuntimeException, CharSequence, Throwable, Class, String } from '@java/java.lang'
    import { TextStyle, DateTimeFormatter } from '@java/java.time.format'
    import { Locale, List, Set, Map, Comparator } from '@java/java.util'
    import { Serializable } from '@java/java.io'
    import { ChronoPeriod, ChronoLocalDate, IsoChronology, ChronoLocalDateTime, IsoEra, ChronoZonedDateTime } from '@java/java.time.chrono'
    import { Stream } from '@java/java.util.stream'
    import { ZoneRules } from '@java/java.time.zone'
    import { ValueRange, TemporalAccessor, TemporalField, Temporal, TemporalUnit, TemporalAmount, TemporalAdjuster, TemporalQuery } from '@java/java.time.temporal'
    export abstract class Clock implements InstantSource {

        static systemUTC(): Clock;

        static systemDefaultZone(): Clock;

        static system(arg0: ZoneId): Clock;

        static tickMillis(arg0: ZoneId): Clock;

        static tickSeconds(arg0: ZoneId): Clock;

        static tickMinutes(arg0: ZoneId): Clock;

        static tick(arg0: Clock, arg1: Duration): Clock;

        static fixed(arg0: Instant, arg1: ZoneId): Clock;

        static offset(arg0: Clock, arg1: Duration): Clock;

        abstract getZone(): ZoneId;

        abstract withZone(arg0: ZoneId): Clock;

        millis(): number;

        abstract instant(): Instant;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class DateTimeException extends RuntimeException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export class DayOfWeek extends Enum<DayOfWeek> implements TemporalAccessor, TemporalAdjuster {
        static MONDAY: DayOfWeek
        static TUESDAY: DayOfWeek
        static WEDNESDAY: DayOfWeek
        static THURSDAY: DayOfWeek
        static FRIDAY: DayOfWeek
        static SATURDAY: DayOfWeek
        static SUNDAY: DayOfWeek

        static values(): DayOfWeek[];

        static valueOf(arg0: String): DayOfWeek;

        static of(arg0: number): DayOfWeek;

        static from(arg0: TemporalAccessor): DayOfWeek;

        getValue(): number;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;

        isSupported(arg0: TemporalField): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        plus(arg0: number): DayOfWeek;

        minus(arg0: number): DayOfWeek;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class Duration extends Object implements TemporalAmount, Comparable<Duration>, Serializable {
        static ZERO: Duration

        static ofDays(arg0: number): Duration;

        static ofHours(arg0: number): Duration;

        static ofMinutes(arg0: number): Duration;

        static ofSeconds(arg0: number): Duration;

        static ofSeconds(arg0: number, arg1: number): Duration;

        static ofMillis(arg0: number): Duration;

        static ofNanos(arg0: number): Duration;

        static of(arg0: number, arg1: TemporalUnit): Duration;

        static from(arg0: TemporalAmount): Duration;

        static parse(arg0: CharSequence): Duration;

        static between(arg0: Temporal, arg1: Temporal): Duration;

        get(arg0: TemporalUnit): number;

        getUnits(): List<TemporalUnit>;

        isZero(): boolean;

        isNegative(): boolean;

        getSeconds(): number;

        getNano(): number;

        withSeconds(arg0: number): Duration;

        withNanos(arg0: number): Duration;

        plus(arg0: Duration): Duration;

        plus(arg0: number, arg1: TemporalUnit): Duration;

        plusDays(arg0: number): Duration;

        plusHours(arg0: number): Duration;

        plusMinutes(arg0: number): Duration;

        plusSeconds(arg0: number): Duration;

        plusMillis(arg0: number): Duration;

        plusNanos(arg0: number): Duration;

        minus(arg0: Duration): Duration;

        minus(arg0: number, arg1: TemporalUnit): Duration;

        minusDays(arg0: number): Duration;

        minusHours(arg0: number): Duration;

        minusMinutes(arg0: number): Duration;

        minusSeconds(arg0: number): Duration;

        minusMillis(arg0: number): Duration;

        minusNanos(arg0: number): Duration;

        multipliedBy(arg0: number): Duration;

        dividedBy(arg0: number): Duration;

        dividedBy(arg0: Duration): number;

        negated(): Duration;

        abs(): Duration;

        addTo(arg0: Temporal): Temporal;

        subtractFrom(arg0: Temporal): Temporal;

        toDays(): number;

        toHours(): number;

        toMinutes(): number;

        toSeconds(): number;

        toMillis(): number;

        toNanos(): number;

        toDaysPart(): number;

        toHoursPart(): number;

        toMinutesPart(): number;

        toSecondsPart(): number;

        toMillisPart(): number;

        toNanosPart(): number;

        truncatedTo(arg0: TemporalUnit): Duration;

        compareTo(arg0: Duration): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class Instant extends Object implements Temporal, TemporalAdjuster, Comparable<Instant>, Serializable {
        static EPOCH: Instant
        static MIN: Instant
        static MAX: Instant

        static now(): Instant;

        static now(arg0: Clock): Instant;

        static ofEpochSecond(arg0: number): Instant;

        static ofEpochSecond(arg0: number, arg1: number): Instant;

        static ofEpochMilli(arg0: number): Instant;

        static from(arg0: TemporalAccessor): Instant;

        static parse(arg0: CharSequence): Instant;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getEpochSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): Instant;

        with(arg0: TemporalField, arg1: number): Instant;

        truncatedTo(arg0: TemporalUnit): Instant;

        plus(arg0: TemporalAmount): Instant;

        plus(arg0: number, arg1: TemporalUnit): Instant;

        plusSeconds(arg0: number): Instant;

        plusMillis(arg0: number): Instant;

        plusNanos(arg0: number): Instant;

        minus(arg0: TemporalAmount): Instant;

        minus(arg0: number, arg1: TemporalUnit): Instant;

        minusSeconds(arg0: number): Instant;

        minusMillis(arg0: number): Instant;

        minusNanos(arg0: number): Instant;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        atOffset(arg0: ZoneOffset): OffsetDateTime;

        atZone(arg0: ZoneId): ZonedDateTime;

        toEpochMilli(): number;

        compareTo(arg0: Instant): number;

        isAfter(arg0: Instant): boolean;

        isBefore(arg0: Instant): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace InstantSource {
        function
/* default */ system(): InstantSource;
        function
/* default */ tick(arg0: InstantSource, arg1: Duration): InstantSource;
        function
/* default */ fixed(arg0: Instant): InstantSource;
        function
/* default */ offset(arg0: InstantSource, arg1: Duration): InstantSource;
    }

    export interface InstantSource {

        instant(): Instant;

/* default */ millis(): number;

/* default */ withZone(arg0: ZoneId): Clock;
    }

    export class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable {
        static MIN: LocalDate
        static MAX: LocalDate
        static EPOCH: LocalDate

        static now(): LocalDate;

        static now(arg0: ZoneId): LocalDate;

        static now(arg0: Clock): LocalDate;

        static of(arg0: number, arg1: Month, arg2: number): LocalDate;

        static of(arg0: number, arg1: number, arg2: number): LocalDate;

        static ofYearDay(arg0: number, arg1: number): LocalDate;

        static ofInstant(arg0: Instant, arg1: ZoneId): LocalDate;

        static ofEpochDay(arg0: number): LocalDate;

        static from(arg0: TemporalAccessor): LocalDate;

        static parse(arg0: CharSequence): LocalDate;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): LocalDate;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getChronology(): IsoChronology;

        getEra(): IsoEra;

        getYear(): number;

        getMonthValue(): number;

        getMonth(): Month;

        getDayOfMonth(): number;

        getDayOfYear(): number;

        getDayOfWeek(): DayOfWeek;

        isLeapYear(): boolean;

        lengthOfMonth(): number;

        lengthOfYear(): number;

        with(arg0: TemporalAdjuster): LocalDate;

        with(arg0: TemporalField, arg1: number): LocalDate;

        withYear(arg0: number): LocalDate;

        withMonth(arg0: number): LocalDate;

        withDayOfMonth(arg0: number): LocalDate;

        withDayOfYear(arg0: number): LocalDate;

        plus(arg0: TemporalAmount): LocalDate;

        plus(arg0: number, arg1: TemporalUnit): LocalDate;

        plusYears(arg0: number): LocalDate;

        plusMonths(arg0: number): LocalDate;

        plusWeeks(arg0: number): LocalDate;

        plusDays(arg0: number): LocalDate;

        minus(arg0: TemporalAmount): LocalDate;

        minus(arg0: number, arg1: TemporalUnit): LocalDate;

        minusYears(arg0: number): LocalDate;

        minusMonths(arg0: number): LocalDate;

        minusWeeks(arg0: number): LocalDate;

        minusDays(arg0: number): LocalDate;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        until(arg0: ChronoLocalDate): Period;

        datesUntil(arg0: LocalDate): Stream<LocalDate>;

        datesUntil(arg0: LocalDate, arg1: Period): Stream<LocalDate>;

        format(arg0: DateTimeFormatter): String;

        atTime(arg0: LocalTime): LocalDateTime;

        atTime(arg0: number, arg1: number): LocalDateTime;

        atTime(arg0: number, arg1: number, arg2: number): LocalDateTime;

        atTime(arg0: number, arg1: number, arg2: number, arg3: number): LocalDateTime;

        atTime(arg0: OffsetTime): OffsetDateTime;

        atStartOfDay(): LocalDateTime;

        atStartOfDay(arg0: ZoneId): ZonedDateTime;

        toEpochDay(): number;

        toEpochSecond(arg0: LocalTime, arg1: ZoneOffset): number;

        compareTo(arg0: ChronoLocalDate): number;

        isAfter(arg0: ChronoLocalDate): boolean;

        isBefore(arg0: ChronoLocalDate): boolean;

        isEqual(arg0: ChronoLocalDate): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface LocalDateTime extends Temporal, TemporalAdjuster, ChronoLocalDateTime<LocalDate>, Serializable { }
    export class LocalDateTime extends Object implements Temporal, TemporalAdjuster, ChronoLocalDateTime<LocalDate>, Serializable {
        static MIN: LocalDateTime
        static MAX: LocalDateTime

        static now(): LocalDateTime;

        static now(arg0: ZoneId): LocalDateTime;

        static now(arg0: Clock): LocalDateTime;

        static of(arg0: number, arg1: Month, arg2: number, arg3: number, arg4: number): LocalDateTime;

        static of(arg0: number, arg1: Month, arg2: number, arg3: number, arg4: number, arg5: number): LocalDateTime;

        static of(arg0: number, arg1: Month, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number): LocalDateTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number): LocalDateTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number): LocalDateTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number): LocalDateTime;

        static of(arg0: LocalDate, arg1: LocalTime): LocalDateTime;

        static ofInstant(arg0: Instant, arg1: ZoneId): LocalDateTime;

        static ofEpochSecond(arg0: number, arg1: number, arg2: ZoneOffset): LocalDateTime;

        static from(arg0: TemporalAccessor): LocalDateTime;

        static parse(arg0: CharSequence): LocalDateTime;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): LocalDateTime;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        toLocalDate(): LocalDate;

        getYear(): number;

        getMonthValue(): number;

        getMonth(): Month;

        getDayOfMonth(): number;

        getDayOfYear(): number;

        getDayOfWeek(): DayOfWeek;

        toLocalTime(): LocalTime;

        getHour(): number;

        getMinute(): number;

        getSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): LocalDateTime;

        with(arg0: TemporalField, arg1: number): LocalDateTime;

        withYear(arg0: number): LocalDateTime;

        withMonth(arg0: number): LocalDateTime;

        withDayOfMonth(arg0: number): LocalDateTime;

        withDayOfYear(arg0: number): LocalDateTime;

        withHour(arg0: number): LocalDateTime;

        withMinute(arg0: number): LocalDateTime;

        withSecond(arg0: number): LocalDateTime;

        withNano(arg0: number): LocalDateTime;

        truncatedTo(arg0: TemporalUnit): LocalDateTime;

        plus(arg0: TemporalAmount): LocalDateTime;

        plus(arg0: number, arg1: TemporalUnit): LocalDateTime;

        plusYears(arg0: number): LocalDateTime;

        plusMonths(arg0: number): LocalDateTime;

        plusWeeks(arg0: number): LocalDateTime;

        plusDays(arg0: number): LocalDateTime;

        plusHours(arg0: number): LocalDateTime;

        plusMinutes(arg0: number): LocalDateTime;

        plusSeconds(arg0: number): LocalDateTime;

        plusNanos(arg0: number): LocalDateTime;

        minus(arg0: TemporalAmount): LocalDateTime;

        minus(arg0: number, arg1: TemporalUnit): LocalDateTime;

        minusYears(arg0: number): LocalDateTime;

        minusMonths(arg0: number): LocalDateTime;

        minusWeeks(arg0: number): LocalDateTime;

        minusDays(arg0: number): LocalDateTime;

        minusHours(arg0: number): LocalDateTime;

        minusMinutes(arg0: number): LocalDateTime;

        minusSeconds(arg0: number): LocalDateTime;

        minusNanos(arg0: number): LocalDateTime;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atOffset(arg0: ZoneOffset): OffsetDateTime;

        atZone(arg0: ZoneId): ZonedDateTime;

        compareTo(arg0: ChronoLocalDateTime<any>): number;

        isAfter(arg0: ChronoLocalDateTime<any>): boolean;

        isBefore(arg0: ChronoLocalDateTime<any>): boolean;

        isEqual(arg0: ChronoLocalDateTime<any>): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class LocalTime extends Object implements Temporal, TemporalAdjuster, Comparable<LocalTime>, Serializable {
        static MIN: LocalTime
        static MAX: LocalTime
        static MIDNIGHT: LocalTime
        static NOON: LocalTime

        static now(): LocalTime;

        static now(arg0: ZoneId): LocalTime;

        static now(arg0: Clock): LocalTime;

        static of(arg0: number, arg1: number): LocalTime;

        static of(arg0: number, arg1: number, arg2: number): LocalTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number): LocalTime;

        static ofInstant(arg0: Instant, arg1: ZoneId): LocalTime;

        static ofSecondOfDay(arg0: number): LocalTime;

        static ofNanoOfDay(arg0: number): LocalTime;

        static from(arg0: TemporalAccessor): LocalTime;

        static parse(arg0: CharSequence): LocalTime;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): LocalTime;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getHour(): number;

        getMinute(): number;

        getSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): LocalTime;

        with(arg0: TemporalField, arg1: number): LocalTime;

        withHour(arg0: number): LocalTime;

        withMinute(arg0: number): LocalTime;

        withSecond(arg0: number): LocalTime;

        withNano(arg0: number): LocalTime;

        truncatedTo(arg0: TemporalUnit): LocalTime;

        plus(arg0: TemporalAmount): LocalTime;

        plus(arg0: number, arg1: TemporalUnit): LocalTime;

        plusHours(arg0: number): LocalTime;

        plusMinutes(arg0: number): LocalTime;

        plusSeconds(arg0: number): LocalTime;

        plusNanos(arg0: number): LocalTime;

        minus(arg0: TemporalAmount): LocalTime;

        minus(arg0: number, arg1: TemporalUnit): LocalTime;

        minusHours(arg0: number): LocalTime;

        minusMinutes(arg0: number): LocalTime;

        minusSeconds(arg0: number): LocalTime;

        minusNanos(arg0: number): LocalTime;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atDate(arg0: LocalDate): LocalDateTime;

        atOffset(arg0: ZoneOffset): OffsetTime;

        toSecondOfDay(): number;

        toNanoOfDay(): number;

        toEpochSecond(arg0: LocalDate, arg1: ZoneOffset): number;

        compareTo(arg0: LocalTime): number;

        isAfter(arg0: LocalTime): boolean;

        isBefore(arg0: LocalTime): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class Month extends Enum<Month> implements TemporalAccessor, TemporalAdjuster {
        static JANUARY: Month
        static FEBRUARY: Month
        static MARCH: Month
        static APRIL: Month
        static MAY: Month
        static JUNE: Month
        static JULY: Month
        static AUGUST: Month
        static SEPTEMBER: Month
        static OCTOBER: Month
        static NOVEMBER: Month
        static DECEMBER: Month

        static values(): Month[];

        static valueOf(arg0: String): Month;

        static of(arg0: number): Month;

        static from(arg0: TemporalAccessor): Month;

        getValue(): number;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;

        isSupported(arg0: TemporalField): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        plus(arg0: number): Month;

        minus(arg0: number): Month;

        length(arg0: boolean): number;

        minLength(): number;

        maxLength(): number;

        firstDayOfYear(arg0: boolean): number;

        firstMonthOfQuarter(): Month;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class MonthDay extends Object implements TemporalAccessor, TemporalAdjuster, Comparable<MonthDay>, Serializable {

        static now(): MonthDay;

        static now(arg0: ZoneId): MonthDay;

        static now(arg0: Clock): MonthDay;

        static of(arg0: Month, arg1: number): MonthDay;

        static of(arg0: number, arg1: number): MonthDay;

        static from(arg0: TemporalAccessor): MonthDay;

        static parse(arg0: CharSequence): MonthDay;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): MonthDay;

        isSupported(arg0: TemporalField): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getMonthValue(): number;

        getMonth(): Month;

        getDayOfMonth(): number;

        isValidYear(arg0: number): boolean;

        withMonth(arg0: number): MonthDay;

        with(arg0: Month): MonthDay;

        withDayOfMonth(arg0: number): MonthDay;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        format(arg0: DateTimeFormatter): String;

        atYear(arg0: number): LocalDate;

        compareTo(arg0: MonthDay): number;

        isAfter(arg0: MonthDay): boolean;

        isBefore(arg0: MonthDay): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class OffsetDateTime extends Object implements Temporal, TemporalAdjuster, Comparable<OffsetDateTime>, Serializable {
        static MIN: OffsetDateTime
        static MAX: OffsetDateTime

        static timeLineOrder(): Comparator<OffsetDateTime>;

        static now(): OffsetDateTime;

        static now(arg0: ZoneId): OffsetDateTime;

        static now(arg0: Clock): OffsetDateTime;

        static of(arg0: LocalDate, arg1: LocalTime, arg2: ZoneOffset): OffsetDateTime;

        static of(arg0: LocalDateTime, arg1: ZoneOffset): OffsetDateTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: ZoneOffset): OffsetDateTime;

        static ofInstant(arg0: Instant, arg1: ZoneId): OffsetDateTime;

        static from(arg0: TemporalAccessor): OffsetDateTime;

        static parse(arg0: CharSequence): OffsetDateTime;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): OffsetDateTime;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getOffset(): ZoneOffset;

        withOffsetSameLocal(arg0: ZoneOffset): OffsetDateTime;

        withOffsetSameInstant(arg0: ZoneOffset): OffsetDateTime;

        toLocalDateTime(): LocalDateTime;

        toLocalDate(): LocalDate;

        getYear(): number;

        getMonthValue(): number;

        getMonth(): Month;

        getDayOfMonth(): number;

        getDayOfYear(): number;

        getDayOfWeek(): DayOfWeek;

        toLocalTime(): LocalTime;

        getHour(): number;

        getMinute(): number;

        getSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): OffsetDateTime;

        with(arg0: TemporalField, arg1: number): OffsetDateTime;

        withYear(arg0: number): OffsetDateTime;

        withMonth(arg0: number): OffsetDateTime;

        withDayOfMonth(arg0: number): OffsetDateTime;

        withDayOfYear(arg0: number): OffsetDateTime;

        withHour(arg0: number): OffsetDateTime;

        withMinute(arg0: number): OffsetDateTime;

        withSecond(arg0: number): OffsetDateTime;

        withNano(arg0: number): OffsetDateTime;

        truncatedTo(arg0: TemporalUnit): OffsetDateTime;

        plus(arg0: TemporalAmount): OffsetDateTime;

        plus(arg0: number, arg1: TemporalUnit): OffsetDateTime;

        plusYears(arg0: number): OffsetDateTime;

        plusMonths(arg0: number): OffsetDateTime;

        plusWeeks(arg0: number): OffsetDateTime;

        plusDays(arg0: number): OffsetDateTime;

        plusHours(arg0: number): OffsetDateTime;

        plusMinutes(arg0: number): OffsetDateTime;

        plusSeconds(arg0: number): OffsetDateTime;

        plusNanos(arg0: number): OffsetDateTime;

        minus(arg0: TemporalAmount): OffsetDateTime;

        minus(arg0: number, arg1: TemporalUnit): OffsetDateTime;

        minusYears(arg0: number): OffsetDateTime;

        minusMonths(arg0: number): OffsetDateTime;

        minusWeeks(arg0: number): OffsetDateTime;

        minusDays(arg0: number): OffsetDateTime;

        minusHours(arg0: number): OffsetDateTime;

        minusMinutes(arg0: number): OffsetDateTime;

        minusSeconds(arg0: number): OffsetDateTime;

        minusNanos(arg0: number): OffsetDateTime;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atZoneSameInstant(arg0: ZoneId): ZonedDateTime;

        atZoneSimilarLocal(arg0: ZoneId): ZonedDateTime;

        toOffsetTime(): OffsetTime;

        toZonedDateTime(): ZonedDateTime;

        toInstant(): Instant;

        toEpochSecond(): number;

        compareTo(arg0: OffsetDateTime): number;

        isAfter(arg0: OffsetDateTime): boolean;

        isBefore(arg0: OffsetDateTime): boolean;

        isEqual(arg0: OffsetDateTime): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class OffsetTime extends Object implements Temporal, TemporalAdjuster, Comparable<OffsetTime>, Serializable {
        static MIN: OffsetTime
        static MAX: OffsetTime

        static now(): OffsetTime;

        static now(arg0: ZoneId): OffsetTime;

        static now(arg0: Clock): OffsetTime;

        static of(arg0: LocalTime, arg1: ZoneOffset): OffsetTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: ZoneOffset): OffsetTime;

        static ofInstant(arg0: Instant, arg1: ZoneId): OffsetTime;

        static from(arg0: TemporalAccessor): OffsetTime;

        static parse(arg0: CharSequence): OffsetTime;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): OffsetTime;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getOffset(): ZoneOffset;

        withOffsetSameLocal(arg0: ZoneOffset): OffsetTime;

        withOffsetSameInstant(arg0: ZoneOffset): OffsetTime;

        toLocalTime(): LocalTime;

        getHour(): number;

        getMinute(): number;

        getSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): OffsetTime;

        with(arg0: TemporalField, arg1: number): OffsetTime;

        withHour(arg0: number): OffsetTime;

        withMinute(arg0: number): OffsetTime;

        withSecond(arg0: number): OffsetTime;

        withNano(arg0: number): OffsetTime;

        truncatedTo(arg0: TemporalUnit): OffsetTime;

        plus(arg0: TemporalAmount): OffsetTime;

        plus(arg0: number, arg1: TemporalUnit): OffsetTime;

        plusHours(arg0: number): OffsetTime;

        plusMinutes(arg0: number): OffsetTime;

        plusSeconds(arg0: number): OffsetTime;

        plusNanos(arg0: number): OffsetTime;

        minus(arg0: TemporalAmount): OffsetTime;

        minus(arg0: number, arg1: TemporalUnit): OffsetTime;

        minusHours(arg0: number): OffsetTime;

        minusMinutes(arg0: number): OffsetTime;

        minusSeconds(arg0: number): OffsetTime;

        minusNanos(arg0: number): OffsetTime;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atDate(arg0: LocalDate): OffsetDateTime;

        toEpochSecond(arg0: LocalDate): number;

        compareTo(arg0: OffsetTime): number;

        isAfter(arg0: OffsetTime): boolean;

        isBefore(arg0: OffsetTime): boolean;

        isEqual(arg0: OffsetTime): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class Period implements ChronoPeriod, Serializable {
        static ZERO: Period

        static ofYears(arg0: number): Period;

        static ofMonths(arg0: number): Period;

        static ofWeeks(arg0: number): Period;

        static ofDays(arg0: number): Period;

        static of(arg0: number, arg1: number, arg2: number): Period;

        static from(arg0: TemporalAmount): Period;

        static parse(arg0: CharSequence): Period;

        static between(arg0: LocalDate, arg1: LocalDate): Period;

        get(arg0: TemporalUnit): number;

        getUnits(): List<TemporalUnit>;

        getChronology(): IsoChronology;

        isZero(): boolean;

        isNegative(): boolean;

        getYears(): number;

        getMonths(): number;

        getDays(): number;

        withYears(arg0: number): Period;

        withMonths(arg0: number): Period;

        withDays(arg0: number): Period;

        plus(arg0: TemporalAmount): Period;

        plusYears(arg0: number): Period;

        plusMonths(arg0: number): Period;

        plusDays(arg0: number): Period;

        minus(arg0: TemporalAmount): Period;

        minusYears(arg0: number): Period;

        minusMonths(arg0: number): Period;

        minusDays(arg0: number): Period;

        multipliedBy(arg0: number): Period;

        negated(): Period;

        normalized(): Period;

        toTotalMonths(): number;

        addTo(arg0: Temporal): Temporal;

        subtractFrom(arg0: Temporal): Temporal;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class Year extends Object implements Temporal, TemporalAdjuster, Comparable<Year>, Serializable {
        static MIN_VALUE: number
        static MAX_VALUE: number

        static now(): Year;

        static now(arg0: ZoneId): Year;

        static now(arg0: Clock): Year;

        static of(arg0: number): Year;

        static from(arg0: TemporalAccessor): Year;

        static parse(arg0: CharSequence): Year;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): Year;

        static isLeap(arg0: number): boolean;

        getValue(): number;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        isLeap(): boolean;

        isValidMonthDay(arg0: MonthDay): boolean;

        length(): number;

        with(arg0: TemporalAdjuster): Year;

        with(arg0: TemporalField, arg1: number): Year;

        plus(arg0: TemporalAmount): Year;

        plus(arg0: number, arg1: TemporalUnit): Year;

        plusYears(arg0: number): Year;

        minus(arg0: TemporalAmount): Year;

        minus(arg0: number, arg1: TemporalUnit): Year;

        minusYears(arg0: number): Year;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atDay(arg0: number): LocalDate;

        atMonth(arg0: Month): YearMonth;

        atMonth(arg0: number): YearMonth;

        atMonthDay(arg0: MonthDay): LocalDate;

        compareTo(arg0: Year): number;

        isAfter(arg0: Year): boolean;

        isBefore(arg0: Year): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class YearMonth extends Object implements Temporal, TemporalAdjuster, Comparable<YearMonth>, Serializable {

        static now(): YearMonth;

        static now(arg0: ZoneId): YearMonth;

        static now(arg0: Clock): YearMonth;

        static of(arg0: number, arg1: Month): YearMonth;

        static of(arg0: number, arg1: number): YearMonth;

        static from(arg0: TemporalAccessor): YearMonth;

        static parse(arg0: CharSequence): YearMonth;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): YearMonth;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getYear(): number;

        getMonthValue(): number;

        getMonth(): Month;

        isLeapYear(): boolean;

        isValidDay(arg0: number): boolean;

        lengthOfMonth(): number;

        lengthOfYear(): number;

        with(arg0: TemporalAdjuster): YearMonth;

        with(arg0: TemporalField, arg1: number): YearMonth;

        withYear(arg0: number): YearMonth;

        withMonth(arg0: number): YearMonth;

        plus(arg0: TemporalAmount): YearMonth;

        plus(arg0: number, arg1: TemporalUnit): YearMonth;

        plusYears(arg0: number): YearMonth;

        plusMonths(arg0: number): YearMonth;

        minus(arg0: TemporalAmount): YearMonth;

        minus(arg0: number, arg1: TemporalUnit): YearMonth;

        minusYears(arg0: number): YearMonth;

        minusMonths(arg0: number): YearMonth;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        atDay(arg0: number): LocalDate;

        atEndOfMonth(): LocalDate;

        compareTo(arg0: YearMonth): number;

        isAfter(arg0: YearMonth): boolean;

        isBefore(arg0: YearMonth): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export abstract class ZoneId implements Serializable {
        static SHORT_IDS: Map<String, String>

        static systemDefault(): ZoneId;

        static getAvailableZoneIds(): Set<String>;

        static of(arg0: String, arg1: Map<String, String>): ZoneId;

        static of(arg0: String): ZoneId;

        static ofOffset(arg0: String, arg1: ZoneOffset): ZoneId;

        static from(arg0: TemporalAccessor): ZoneId;

        abstract getId(): String;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;

        abstract getRules(): ZoneRules;

        normalized(): ZoneId;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class ZoneOffset extends ZoneId implements TemporalAccessor, TemporalAdjuster, Comparable<ZoneOffset>, Serializable {
        static UTC: ZoneOffset
        static MIN: ZoneOffset
        static MAX: ZoneOffset

        static of(arg0: String): ZoneOffset;

        static ofHours(arg0: number): ZoneOffset;

        static ofHoursMinutes(arg0: number, arg1: number): ZoneOffset;

        static ofHoursMinutesSeconds(arg0: number, arg1: number, arg2: number): ZoneOffset;

        static from(arg0: TemporalAccessor): ZoneOffset;

        static ofTotalSeconds(arg0: number): ZoneOffset;

        getTotalSeconds(): number;

        getId(): String;

        getRules(): ZoneRules;

        isSupported(arg0: TemporalField): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        adjustInto(arg0: Temporal): Temporal;

        compareTo(arg0: ZoneOffset): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface ZonedDateTime extends Temporal, ChronoZonedDateTime<LocalDate>, Serializable { }
    export class ZonedDateTime extends Object implements Temporal, ChronoZonedDateTime<LocalDate>, Serializable {

        static now(): ZonedDateTime;

        static now(arg0: ZoneId): ZonedDateTime;

        static now(arg0: Clock): ZonedDateTime;

        static of(arg0: LocalDate, arg1: LocalTime, arg2: ZoneId): ZonedDateTime;

        static of(arg0: LocalDateTime, arg1: ZoneId): ZonedDateTime;

        static of(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: ZoneId): ZonedDateTime;

        static ofLocal(arg0: LocalDateTime, arg1: ZoneId, arg2: ZoneOffset): ZonedDateTime;

        static ofInstant(arg0: Instant, arg1: ZoneId): ZonedDateTime;

        static ofInstant(arg0: LocalDateTime, arg1: ZoneOffset, arg2: ZoneId): ZonedDateTime;

        static ofStrict(arg0: LocalDateTime, arg1: ZoneOffset, arg2: ZoneId): ZonedDateTime;

        static from(arg0: TemporalAccessor): ZonedDateTime;

        static parse(arg0: CharSequence): ZonedDateTime;

        static parse(arg0: CharSequence, arg1: DateTimeFormatter): ZonedDateTime;

        isSupported(arg0: TemporalField): boolean;

        isSupported(arg0: TemporalUnit): boolean;

        range(arg0: TemporalField): ValueRange;

        get(arg0: TemporalField): number;

        getLong(arg0: TemporalField): number;

        getOffset(): ZoneOffset;

        withEarlierOffsetAtOverlap(): ZonedDateTime;

        withLaterOffsetAtOverlap(): ZonedDateTime;

        getZone(): ZoneId;

        withZoneSameLocal(arg0: ZoneId): ZonedDateTime;

        withZoneSameInstant(arg0: ZoneId): ZonedDateTime;

        withFixedOffsetZone(): ZonedDateTime;

        toLocalDateTime(): LocalDateTime;

        toLocalDate(): LocalDate;

        getYear(): number;

        getMonthValue(): number;

        getMonth(): Month;

        getDayOfMonth(): number;

        getDayOfYear(): number;

        getDayOfWeek(): DayOfWeek;

        toLocalTime(): LocalTime;

        getHour(): number;

        getMinute(): number;

        getSecond(): number;

        getNano(): number;

        with(arg0: TemporalAdjuster): ZonedDateTime;

        with(arg0: TemporalField, arg1: number): ZonedDateTime;

        withYear(arg0: number): ZonedDateTime;

        withMonth(arg0: number): ZonedDateTime;

        withDayOfMonth(arg0: number): ZonedDateTime;

        withDayOfYear(arg0: number): ZonedDateTime;

        withHour(arg0: number): ZonedDateTime;

        withMinute(arg0: number): ZonedDateTime;

        withSecond(arg0: number): ZonedDateTime;

        withNano(arg0: number): ZonedDateTime;

        truncatedTo(arg0: TemporalUnit): ZonedDateTime;

        plus(arg0: TemporalAmount): ZonedDateTime;

        plus(arg0: number, arg1: TemporalUnit): ZonedDateTime;

        plusYears(arg0: number): ZonedDateTime;

        plusMonths(arg0: number): ZonedDateTime;

        plusWeeks(arg0: number): ZonedDateTime;

        plusDays(arg0: number): ZonedDateTime;

        plusHours(arg0: number): ZonedDateTime;

        plusMinutes(arg0: number): ZonedDateTime;

        plusSeconds(arg0: number): ZonedDateTime;

        plusNanos(arg0: number): ZonedDateTime;

        minus(arg0: TemporalAmount): ZonedDateTime;

        minus(arg0: number, arg1: TemporalUnit): ZonedDateTime;

        minusYears(arg0: number): ZonedDateTime;

        minusMonths(arg0: number): ZonedDateTime;

        minusWeeks(arg0: number): ZonedDateTime;

        minusDays(arg0: number): ZonedDateTime;

        minusHours(arg0: number): ZonedDateTime;

        minusMinutes(arg0: number): ZonedDateTime;

        minusSeconds(arg0: number): ZonedDateTime;

        minusNanos(arg0: number): ZonedDateTime;

        query<R extends Object>(arg0: TemporalQuery<R>): R;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        format(arg0: DateTimeFormatter): String;

        toOffsetDateTime(): OffsetDateTime;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.format.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.time.temporal.d.ts" />
declare module '@java/java.time.chrono' {
    import { Locale, List, Set, Map, Comparator } from '@java/java.util'
    import { Long, Enum, Comparable, Class, String } from '@java/java.lang'
    import { ResolverStyle, DateTimeFormatter, TextStyle } from '@java/java.time.format'
    import { LocalDateTime, LocalTime, ZoneId, ZonedDateTime, Instant, Period, Clock, LocalDate, ZoneOffset } from '@java/java.time'
    import { Serializable } from '@java/java.io'
    import { ValueRange, TemporalField, TemporalAccessor, Temporal, TemporalUnit, TemporalAmount, ChronoField, TemporalAdjuster, TemporalQuery } from '@java/java.time.temporal'
    export interface AbstractChronology extends Chronology { }
    export abstract class AbstractChronology implements Chronology {

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): ChronoLocalDate;

        compareTo(arg0: Chronology): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace ChronoLocalDate {
        function
/* default */ timeLineOrder(): Comparator<ChronoLocalDate>;
        function
/* default */ from(arg0: TemporalAccessor): ChronoLocalDate;
    }

    export interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate>, Object {

        getChronology(): Chronology;

/* default */ getEra(): Era;

/* default */ isLeapYear(): boolean;

        lengthOfMonth(): number;

/* default */ lengthOfYear(): number;

/* default */ isSupported(arg0: TemporalField): boolean;

/* default */ isSupported(arg0: TemporalUnit): boolean;

/* default */ with(arg0: TemporalAdjuster): ChronoLocalDate;

/* default */ with(arg0: TemporalField, arg1: number): ChronoLocalDate;

/* default */ plus(arg0: TemporalAmount): ChronoLocalDate;

/* default */ plus(arg0: number, arg1: TemporalUnit): ChronoLocalDate;

/* default */ minus(arg0: TemporalAmount): ChronoLocalDate;

/* default */ minus(arg0: number, arg1: TemporalUnit): ChronoLocalDate;

/* default */ query<R extends Object>(arg0: TemporalQuery<R>): R;

/* default */ adjustInto(arg0: Temporal): Temporal;

        until(arg0: Temporal, arg1: TemporalUnit): number;

        until(arg0: ChronoLocalDate): ChronoPeriod;

/* default */ format(arg0: DateTimeFormatter): String;

/* default */ atTime(arg0: LocalTime): ChronoLocalDateTime<any>;

/* default */ toEpochDay(): number;

/* default */ compareTo(arg0: ChronoLocalDate): number;

/* default */ isAfter(arg0: ChronoLocalDate): boolean;

/* default */ isBefore(arg0: ChronoLocalDate): boolean;

/* default */ isEqual(arg0: ChronoLocalDate): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace ChronoLocalDateTime {
        function
/* default */ timeLineOrder(): Comparator<ChronoLocalDateTime<any>>;
        function
/* default */ from(arg0: TemporalAccessor): ChronoLocalDateTime<any>;
    }

    export interface ChronoLocalDateTime<D extends ChronoLocalDate> extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime<any>>, Object {

/* default */ getChronology(): Chronology;

        toLocalDate(): D;

        toLocalTime(): LocalTime;

        isSupported(arg0: TemporalField): boolean;

/* default */ isSupported(arg0: TemporalUnit): boolean;

/* default */ with(arg0: TemporalAdjuster): ChronoLocalDateTime<D>;

        with(arg0: TemporalField, arg1: number): ChronoLocalDateTime<D>;

/* default */ plus(arg0: TemporalAmount): ChronoLocalDateTime<D>;

        plus(arg0: number, arg1: TemporalUnit): ChronoLocalDateTime<D>;

/* default */ minus(arg0: TemporalAmount): ChronoLocalDateTime<D>;

/* default */ minus(arg0: number, arg1: TemporalUnit): ChronoLocalDateTime<D>;

/* default */ query<R extends Object>(arg0: TemporalQuery<R>): R;

/* default */ adjustInto(arg0: Temporal): Temporal;

/* default */ format(arg0: DateTimeFormatter): String;

        atZone(arg0: ZoneId): ChronoZonedDateTime<D>;

/* default */ toInstant(arg0: ZoneOffset): Instant;

/* default */ toEpochSecond(arg0: ZoneOffset): number;

/* default */ compareTo(arg0: ChronoLocalDateTime<any>): number;

/* default */ isAfter(arg0: ChronoLocalDateTime<any>): boolean;

/* default */ isBefore(arg0: ChronoLocalDateTime<any>): boolean;

/* default */ isEqual(arg0: ChronoLocalDateTime<any>): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace ChronoPeriod {
        function
/* default */ between(arg0: ChronoLocalDate, arg1: ChronoLocalDate): ChronoPeriod;
    }

    export interface ChronoPeriod extends TemporalAmount {

        get(arg0: TemporalUnit): number;

        getUnits(): List<TemporalUnit>;

        getChronology(): Chronology;

/* default */ isZero(): boolean;

/* default */ isNegative(): boolean;

        plus(arg0: TemporalAmount): ChronoPeriod;

        minus(arg0: TemporalAmount): ChronoPeriod;

        multipliedBy(arg0: number): ChronoPeriod;

/* default */ negated(): ChronoPeriod;

        normalized(): ChronoPeriod;

        addTo(arg0: Temporal): Temporal;

        subtractFrom(arg0: Temporal): Temporal;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace ChronoZonedDateTime {
        function
/* default */ timeLineOrder(): Comparator<ChronoZonedDateTime<any>>;
        function
/* default */ from(arg0: TemporalAccessor): ChronoZonedDateTime<any>;
    }

    export interface ChronoZonedDateTime<D extends ChronoLocalDate> extends Temporal, Comparable<ChronoZonedDateTime<any>>, Object {

/* default */ range(arg0: TemporalField): ValueRange;

/* default */ get(arg0: TemporalField): number;

/* default */ getLong(arg0: TemporalField): number;

/* default */ toLocalDate(): D;

/* default */ toLocalTime(): LocalTime;

        toLocalDateTime(): ChronoLocalDateTime<D>;

/* default */ getChronology(): Chronology;

        getOffset(): ZoneOffset;

        getZone(): ZoneId;

        withEarlierOffsetAtOverlap(): ChronoZonedDateTime<D>;

        withLaterOffsetAtOverlap(): ChronoZonedDateTime<D>;

        withZoneSameLocal(arg0: ZoneId): ChronoZonedDateTime<D>;

        withZoneSameInstant(arg0: ZoneId): ChronoZonedDateTime<D>;

        isSupported(arg0: TemporalField): boolean;

/* default */ isSupported(arg0: TemporalUnit): boolean;

/* default */ with(arg0: TemporalAdjuster): ChronoZonedDateTime<D>;

        with(arg0: TemporalField, arg1: number): ChronoZonedDateTime<D>;

/* default */ plus(arg0: TemporalAmount): ChronoZonedDateTime<D>;

        plus(arg0: number, arg1: TemporalUnit): ChronoZonedDateTime<D>;

/* default */ minus(arg0: TemporalAmount): ChronoZonedDateTime<D>;

/* default */ minus(arg0: number, arg1: TemporalUnit): ChronoZonedDateTime<D>;

/* default */ query<R extends Object>(arg0: TemporalQuery<R>): R;

/* default */ format(arg0: DateTimeFormatter): String;

/* default */ toInstant(): Instant;

/* default */ toEpochSecond(): number;

/* default */ compareTo(arg0: ChronoZonedDateTime<any>): number;

/* default */ isBefore(arg0: ChronoZonedDateTime<any>): boolean;

/* default */ isAfter(arg0: ChronoZonedDateTime<any>): boolean;

/* default */ isEqual(arg0: ChronoZonedDateTime<any>): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace Chronology {
        function
/* default */ from(arg0: TemporalAccessor): Chronology;
        function
/* default */ ofLocale(arg0: Locale): Chronology;
        function
/* default */ of(arg0: String): Chronology;
        function
/* default */ getAvailableChronologies(): Set<Chronology>;
    }

    export interface Chronology extends Comparable<Chronology>, Object {

        getId(): String;

        getCalendarType(): String;

/* default */ date(arg0: Era, arg1: number, arg2: number, arg3: number): ChronoLocalDate;

        date(arg0: number, arg1: number, arg2: number): ChronoLocalDate;

/* default */ dateYearDay(arg0: Era, arg1: number, arg2: number): ChronoLocalDate;

        dateYearDay(arg0: number, arg1: number): ChronoLocalDate;

        dateEpochDay(arg0: number): ChronoLocalDate;

/* default */ dateNow(): ChronoLocalDate;

/* default */ dateNow(arg0: ZoneId): ChronoLocalDate;

/* default */ dateNow(arg0: Clock): ChronoLocalDate;

        date(arg0: TemporalAccessor): ChronoLocalDate;

/* default */ localDateTime(arg0: TemporalAccessor): ChronoLocalDateTime<ChronoLocalDate>;

/* default */ zonedDateTime(arg0: TemporalAccessor): ChronoZonedDateTime<ChronoLocalDate>;

/* default */ zonedDateTime(arg0: Instant, arg1: ZoneId): ChronoZonedDateTime<ChronoLocalDate>;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): Era;

        eras(): List<Era>;

        range(arg0: ChronoField): ValueRange;

/* default */ getDisplayName(arg0: TextStyle, arg1: Locale): String;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): ChronoLocalDate;

/* default */ period(arg0: number, arg1: number, arg2: number): ChronoPeriod;

/* default */ epochSecond(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: ZoneOffset): number;

/* default */ epochSecond(arg0: Era, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number, arg7: ZoneOffset): number;

        compareTo(arg0: Chronology): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface Era extends TemporalAccessor, TemporalAdjuster {

        getValue(): number;

/* default */ isSupported(arg0: TemporalField): boolean;

/* default */ range(arg0: TemporalField): ValueRange;

/* default */ get(arg0: TemporalField): number;

/* default */ getLong(arg0: TemporalField): number;

/* default */ query<R extends Object>(arg0: TemporalQuery<R>): R;

/* default */ adjustInto(arg0: Temporal): Temporal;

/* default */ getDisplayName(arg0: TextStyle, arg1: Locale): String;
    }

    export interface HijrahChronology extends Serializable { }
    export class HijrahChronology extends AbstractChronology implements Serializable {
        static INSTANCE: HijrahChronology

        getId(): String;

        getCalendarType(): String;

        date(arg0: Era, arg1: number, arg2: number, arg3: number): HijrahDate;

        date(arg0: number, arg1: number, arg2: number): HijrahDate;

        dateYearDay(arg0: Era, arg1: number, arg2: number): HijrahDate;

        dateYearDay(arg0: number, arg1: number): HijrahDate;

        dateEpochDay(arg0: number): HijrahDate;

        dateNow(): HijrahDate;

        dateNow(arg0: ZoneId): HijrahDate;

        dateNow(arg0: Clock): HijrahDate;

        date(arg0: TemporalAccessor): HijrahDate;

        localDateTime(arg0: TemporalAccessor): ChronoLocalDateTime<HijrahDate>;

        zonedDateTime(arg0: TemporalAccessor): ChronoZonedDateTime<HijrahDate>;

        zonedDateTime(arg0: Instant, arg1: ZoneId): ChronoZonedDateTime<HijrahDate>;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): HijrahEra;

        eras(): List<Era>;

        range(arg0: ChronoField): ValueRange;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): HijrahDate;
    }

    export interface HijrahDate extends ChronoLocalDate, Serializable { }
    export class HijrahDate extends ChronoLocalDateImpl<HijrahDate> implements ChronoLocalDate, Serializable {

        static now(): HijrahDate;

        static now(arg0: ZoneId): HijrahDate;

        static now(arg0: Clock): HijrahDate;

        static of(arg0: number, arg1: number, arg2: number): HijrahDate;

        static from(arg0: TemporalAccessor): HijrahDate;

        getChronology(): HijrahChronology;

        getEra(): HijrahEra;

        lengthOfMonth(): number;

        lengthOfYear(): number;

        range(arg0: TemporalField): ValueRange;

        getLong(arg0: TemporalField): number;

        with(arg0: TemporalField, arg1: number): HijrahDate;

        with(arg0: TemporalAdjuster): HijrahDate;

        withVariant(arg0: HijrahChronology): HijrahDate;

        plus(arg0: TemporalAmount): HijrahDate;

        minus(arg0: TemporalAmount): HijrahDate;

        toEpochDay(): number;

        isLeapYear(): boolean;

        plus(arg0: number, arg1: TemporalUnit): HijrahDate;

        minus(arg0: number, arg1: TemporalUnit): HijrahDate;

        atTime(arg0: LocalTime): ChronoLocalDateTime<HijrahDate>;

        until(arg0: ChronoLocalDate): ChronoPeriod;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export interface HijrahEra extends Era { }
    export class HijrahEra extends Enum<HijrahEra> implements Era {
        static AH: HijrahEra

        static values(): HijrahEra[];

        static valueOf(arg0: String): HijrahEra;

        static of(arg0: number): HijrahEra;

        getValue(): number;

        range(arg0: TemporalField): ValueRange;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface IsoChronology extends Serializable { }
    export class IsoChronology extends AbstractChronology implements Serializable {
        static INSTANCE: IsoChronology

        getId(): String;

        getCalendarType(): String;

        date(arg0: Era, arg1: number, arg2: number, arg3: number): LocalDate;

        date(arg0: number, arg1: number, arg2: number): LocalDate;

        dateYearDay(arg0: Era, arg1: number, arg2: number): LocalDate;

        dateYearDay(arg0: number, arg1: number): LocalDate;

        dateEpochDay(arg0: number): LocalDate;

        date(arg0: TemporalAccessor): LocalDate;

        epochSecond(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: ZoneOffset): number;

        localDateTime(arg0: TemporalAccessor): LocalDateTime;

        zonedDateTime(arg0: TemporalAccessor): ZonedDateTime;

        zonedDateTime(arg0: Instant, arg1: ZoneId): ZonedDateTime;

        dateNow(): LocalDate;

        dateNow(arg0: ZoneId): LocalDate;

        dateNow(arg0: Clock): LocalDate;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): IsoEra;

        eras(): List<Era>;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): LocalDate;

        range(arg0: ChronoField): ValueRange;

        period(arg0: number, arg1: number, arg2: number): Period;
    }

    export interface IsoEra extends Era { }
    export class IsoEra extends Enum<IsoEra> implements Era {
        static BCE: IsoEra
        static CE: IsoEra

        static values(): IsoEra[];

        static valueOf(arg0: String): IsoEra;

        static of(arg0: number): IsoEra;

        getValue(): number;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface JapaneseChronology extends Serializable { }
    export class JapaneseChronology extends AbstractChronology implements Serializable {
        static INSTANCE: JapaneseChronology

        getId(): String;

        getCalendarType(): String;

        date(arg0: Era, arg1: number, arg2: number, arg3: number): JapaneseDate;

        date(arg0: number, arg1: number, arg2: number): JapaneseDate;

        dateYearDay(arg0: Era, arg1: number, arg2: number): JapaneseDate;

        dateYearDay(arg0: number, arg1: number): JapaneseDate;

        dateEpochDay(arg0: number): JapaneseDate;

        dateNow(): JapaneseDate;

        dateNow(arg0: ZoneId): JapaneseDate;

        dateNow(arg0: Clock): JapaneseDate;

        date(arg0: TemporalAccessor): JapaneseDate;

        localDateTime(arg0: TemporalAccessor): ChronoLocalDateTime<JapaneseDate>;

        zonedDateTime(arg0: TemporalAccessor): ChronoZonedDateTime<JapaneseDate>;

        zonedDateTime(arg0: Instant, arg1: ZoneId): ChronoZonedDateTime<JapaneseDate>;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): JapaneseEra;

        eras(): List<Era>;

        range(arg0: ChronoField): ValueRange;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): JapaneseDate;
    }

    export interface JapaneseDate extends ChronoLocalDate, Serializable { }
    export class JapaneseDate extends ChronoLocalDateImpl<JapaneseDate> implements ChronoLocalDate, Serializable {

        static now(): JapaneseDate;

        static now(arg0: ZoneId): JapaneseDate;

        static now(arg0: Clock): JapaneseDate;

        static of(arg0: JapaneseEra, arg1: number, arg2: number, arg3: number): JapaneseDate;

        static of(arg0: number, arg1: number, arg2: number): JapaneseDate;

        static from(arg0: TemporalAccessor): JapaneseDate;

        getChronology(): JapaneseChronology;

        getEra(): JapaneseEra;

        lengthOfMonth(): number;

        lengthOfYear(): number;

        isSupported(arg0: TemporalField): boolean;

        range(arg0: TemporalField): ValueRange;

        getLong(arg0: TemporalField): number;

        with(arg0: TemporalField, arg1: number): JapaneseDate;

        with(arg0: TemporalAdjuster): JapaneseDate;

        plus(arg0: TemporalAmount): JapaneseDate;

        minus(arg0: TemporalAmount): JapaneseDate;

        plus(arg0: number, arg1: TemporalUnit): JapaneseDate;

        minus(arg0: number, arg1: TemporalUnit): JapaneseDate;

        atTime(arg0: LocalTime): ChronoLocalDateTime<JapaneseDate>;

        until(arg0: ChronoLocalDate): ChronoPeriod;

        toEpochDay(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export interface JapaneseEra extends Era, Serializable { }
    export class JapaneseEra implements Era, Serializable {
        static MEIJI: JapaneseEra
        static TAISHO: JapaneseEra
        static SHOWA: JapaneseEra
        static HEISEI: JapaneseEra
        static REIWA: JapaneseEra

        static of(arg0: number): JapaneseEra;

        static valueOf(arg0: String): JapaneseEra;

        static values(): JapaneseEra[];

        getDisplayName(arg0: TextStyle, arg1: Locale): String;

        getValue(): number;

        range(arg0: TemporalField): ValueRange;
        toString(): string;
    }

    export interface MinguoChronology extends Serializable { }
    export class MinguoChronology extends AbstractChronology implements Serializable {
        static INSTANCE: MinguoChronology

        getId(): String;

        getCalendarType(): String;

        date(arg0: Era, arg1: number, arg2: number, arg3: number): MinguoDate;

        date(arg0: number, arg1: number, arg2: number): MinguoDate;

        dateYearDay(arg0: Era, arg1: number, arg2: number): MinguoDate;

        dateYearDay(arg0: number, arg1: number): MinguoDate;

        dateEpochDay(arg0: number): MinguoDate;

        dateNow(): MinguoDate;

        dateNow(arg0: ZoneId): MinguoDate;

        dateNow(arg0: Clock): MinguoDate;

        date(arg0: TemporalAccessor): MinguoDate;

        localDateTime(arg0: TemporalAccessor): ChronoLocalDateTime<MinguoDate>;

        zonedDateTime(arg0: TemporalAccessor): ChronoZonedDateTime<MinguoDate>;

        zonedDateTime(arg0: Instant, arg1: ZoneId): ChronoZonedDateTime<MinguoDate>;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): MinguoEra;

        eras(): List<Era>;

        range(arg0: ChronoField): ValueRange;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): MinguoDate;
    }

    export interface MinguoDate extends ChronoLocalDate, Serializable { }
    export class MinguoDate extends ChronoLocalDateImpl<MinguoDate> implements ChronoLocalDate, Serializable {

        static now(): MinguoDate;

        static now(arg0: ZoneId): MinguoDate;

        static now(arg0: Clock): MinguoDate;

        static of(arg0: number, arg1: number, arg2: number): MinguoDate;

        static from(arg0: TemporalAccessor): MinguoDate;

        getChronology(): MinguoChronology;

        getEra(): MinguoEra;

        lengthOfMonth(): number;

        range(arg0: TemporalField): ValueRange;

        getLong(arg0: TemporalField): number;

        with(arg0: TemporalField, arg1: number): MinguoDate;

        with(arg0: TemporalAdjuster): MinguoDate;

        plus(arg0: TemporalAmount): MinguoDate;

        minus(arg0: TemporalAmount): MinguoDate;

        plus(arg0: number, arg1: TemporalUnit): MinguoDate;

        minus(arg0: number, arg1: TemporalUnit): MinguoDate;

        atTime(arg0: LocalTime): ChronoLocalDateTime<MinguoDate>;

        until(arg0: ChronoLocalDate): ChronoPeriod;

        toEpochDay(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export interface MinguoEra extends Era { }
    export class MinguoEra extends Enum<MinguoEra> implements Era {
        static BEFORE_ROC: MinguoEra
        static ROC: MinguoEra

        static values(): MinguoEra[];

        static valueOf(arg0: String): MinguoEra;

        static of(arg0: number): MinguoEra;

        getValue(): number;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface ThaiBuddhistChronology extends Serializable { }
    export class ThaiBuddhistChronology extends AbstractChronology implements Serializable {
        static INSTANCE: ThaiBuddhistChronology

        getId(): String;

        getCalendarType(): String;

        date(arg0: Era, arg1: number, arg2: number, arg3: number): ThaiBuddhistDate;

        date(arg0: number, arg1: number, arg2: number): ThaiBuddhistDate;

        dateYearDay(arg0: Era, arg1: number, arg2: number): ThaiBuddhistDate;

        dateYearDay(arg0: number, arg1: number): ThaiBuddhistDate;

        dateEpochDay(arg0: number): ThaiBuddhistDate;

        dateNow(): ThaiBuddhistDate;

        dateNow(arg0: ZoneId): ThaiBuddhistDate;

        dateNow(arg0: Clock): ThaiBuddhistDate;

        date(arg0: TemporalAccessor): ThaiBuddhistDate;

        localDateTime(arg0: TemporalAccessor): ChronoLocalDateTime<ThaiBuddhistDate>;

        zonedDateTime(arg0: TemporalAccessor): ChronoZonedDateTime<ThaiBuddhistDate>;

        zonedDateTime(arg0: Instant, arg1: ZoneId): ChronoZonedDateTime<ThaiBuddhistDate>;

        isLeapYear(arg0: number): boolean;

        prolepticYear(arg0: Era, arg1: number): number;

        eraOf(arg0: number): ThaiBuddhistEra;

        eras(): List<Era>;

        range(arg0: ChronoField): ValueRange;

        resolveDate(arg0: Map<TemporalField, Number>, arg1: ResolverStyle): ThaiBuddhistDate;
    }

    export interface ThaiBuddhistDate extends ChronoLocalDate, Serializable { }
    export class ThaiBuddhistDate extends ChronoLocalDateImpl<ThaiBuddhistDate> implements ChronoLocalDate, Serializable {

        static now(): ThaiBuddhistDate;

        static now(arg0: ZoneId): ThaiBuddhistDate;

        static now(arg0: Clock): ThaiBuddhistDate;

        static of(arg0: number, arg1: number, arg2: number): ThaiBuddhistDate;

        static from(arg0: TemporalAccessor): ThaiBuddhistDate;

        getChronology(): ThaiBuddhistChronology;

        getEra(): ThaiBuddhistEra;

        lengthOfMonth(): number;

        range(arg0: TemporalField): ValueRange;

        getLong(arg0: TemporalField): number;

        with(arg0: TemporalField, arg1: number): ThaiBuddhistDate;

        with(arg0: TemporalAdjuster): ThaiBuddhistDate;

        plus(arg0: TemporalAmount): ThaiBuddhistDate;

        minus(arg0: TemporalAmount): ThaiBuddhistDate;

        plus(arg0: number, arg1: TemporalUnit): ThaiBuddhistDate;

        minus(arg0: number, arg1: TemporalUnit): ThaiBuddhistDate;

        atTime(arg0: LocalTime): ChronoLocalDateTime<ThaiBuddhistDate>;

        until(arg0: ChronoLocalDate): ChronoPeriod;

        toEpochDay(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export interface ThaiBuddhistEra extends Era { }
    export class ThaiBuddhistEra extends Enum<ThaiBuddhistEra> implements Era {
        static BEFORE_BE: ThaiBuddhistEra
        static BE: ThaiBuddhistEra

        static values(): ThaiBuddhistEra[];

        static valueOf(arg0: String): ThaiBuddhistEra;

        static of(arg0: number): ThaiBuddhistEra;

        getValue(): number;

        getDisplayName(arg0: TextStyle, arg1: Locale): String;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

}
/// <reference path="java.util.spi.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.text.d.ts" />
declare module '@java/java.text.spi' {
    import { LocaleServiceProvider } from '@java/java.util.spi'
    import { Locale } from '@java/java.util'
    import { DateFormatSymbols, DecimalFormatSymbols, BreakIterator, Collator, DateFormat, NumberFormat } from '@java/java.text'
    export abstract class BreakIteratorProvider extends LocaleServiceProvider {

        abstract getWordInstance(arg0: Locale): BreakIterator;

        abstract getLineInstance(arg0: Locale): BreakIterator;

        abstract getCharacterInstance(arg0: Locale): BreakIterator;

        abstract getSentenceInstance(arg0: Locale): BreakIterator;
    }

    export abstract class CollatorProvider extends LocaleServiceProvider {

        abstract getInstance(arg0: Locale): Collator;
    }

    export abstract class DateFormatProvider extends LocaleServiceProvider {

        abstract getTimeInstance(arg0: number, arg1: Locale): DateFormat;

        abstract getDateInstance(arg0: number, arg1: Locale): DateFormat;

        abstract getDateTimeInstance(arg0: number, arg1: number, arg2: Locale): DateFormat;
    }

    export abstract class DateFormatSymbolsProvider extends LocaleServiceProvider {

        abstract getInstance(arg0: Locale): DateFormatSymbols;
    }

    export abstract class DecimalFormatSymbolsProvider extends LocaleServiceProvider {

        abstract getInstance(arg0: Locale): DecimalFormatSymbols;
    }

    export abstract class NumberFormatProvider extends LocaleServiceProvider {

        abstract getCurrencyInstance(arg0: Locale): NumberFormat;

        abstract getIntegerInstance(arg0: Locale): NumberFormat;

        abstract getNumberInstance(arg0: Locale): NumberFormat;

        abstract getPercentInstance(arg0: Locale): NumberFormat;

        getCompactNumberInstance(arg0: Locale, arg1: NumberFormat.Style): NumberFormat;
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.math.d.ts" />
declare module '@java/java.text' {
    import { Locale, TimeZone, Set, Calendar, Currency, Map, Date, Comparator } from '@java/java.util'
    import { Enum, StringBuffer, Comparable, Number, CharSequence, Cloneable, Class, String, Exception } from '@java/java.lang'
    import { Serializable } from '@java/java.io'
    import { RoundingMode } from '@java/java.math'
    export class Annotation {
        constructor(arg0: Object);

        getValue(): Object;
        toString(): string;
    }

    export interface AttributedCharacterIterator extends CharacterIterator {

        getRunStart(): number;

        getRunStart(arg0: AttributedCharacterIterator.Attribute): number;

        getRunStart(arg0: Set<AttributedCharacterIterator.Attribute>): number;

        getRunLimit(): number;

        getRunLimit(arg0: AttributedCharacterIterator.Attribute): number;

        getRunLimit(arg0: Set<AttributedCharacterIterator.Attribute>): number;

        getAttributes(): Map<AttributedCharacterIterator.Attribute, Object>;

        getAttribute(arg0: AttributedCharacterIterator.Attribute): Object;

        getAllAttributeKeys(): Set<AttributedCharacterIterator.Attribute>;
    }
    export namespace AttributedCharacterIterator {
        export class Attribute implements Serializable {
            static LANGUAGE: AttributedCharacterIterator.Attribute
            static READING: AttributedCharacterIterator.Attribute
            static INPUT_METHOD_SEGMENT: AttributedCharacterIterator.Attribute

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

    }

    export class AttributedString {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Map<AttributedCharacterIterator.Attribute, any>);
        constructor(arg0: AttributedCharacterIterator);
        constructor(arg0: AttributedCharacterIterator, arg1: number, arg2: number);
        constructor(arg0: AttributedCharacterIterator, arg1: number, arg2: number, arg3: AttributedCharacterIterator.Attribute[]);

        addAttribute(arg0: AttributedCharacterIterator.Attribute, arg1: Object): void;

        addAttribute(arg0: AttributedCharacterIterator.Attribute, arg1: Object, arg2: number, arg3: number): void;

        addAttributes(arg0: Map<AttributedCharacterIterator.Attribute, any>, arg1: number, arg2: number): void;

        getIterator(): AttributedCharacterIterator;

        getIterator(arg0: AttributedCharacterIterator.Attribute[]): AttributedCharacterIterator;

        getIterator(arg0: AttributedCharacterIterator.Attribute[], arg1: number, arg2: number): AttributedCharacterIterator;
    }

    export class Bidi {
        static DIRECTION_LEFT_TO_RIGHT: number
        static DIRECTION_RIGHT_TO_LEFT: number
        static DIRECTION_DEFAULT_LEFT_TO_RIGHT: number
        static DIRECTION_DEFAULT_RIGHT_TO_LEFT: number
        constructor(arg0: String, arg1: number);
        constructor(arg0: AttributedCharacterIterator);
        constructor(arg0: String[], arg1: number, arg2: number[], arg3: number, arg4: number, arg5: number);

        createLineBidi(arg0: number, arg1: number): Bidi;

        isMixed(): boolean;

        isLeftToRight(): boolean;

        isRightToLeft(): boolean;

        getLength(): number;

        baseIsLeftToRight(): boolean;

        getBaseLevel(): number;

        getLevelAt(arg0: number): number;

        getRunCount(): number;

        getRunLevel(arg0: number): number;

        getRunStart(arg0: number): number;

        getRunLimit(arg0: number): number;

        static requiresBidi(arg0: String[], arg1: number, arg2: number): boolean;

        static reorderVisually(arg0: number[], arg1: number, arg2: Object[], arg3: number, arg4: number): void;
        toString(): string;
    }

    export abstract class BreakIterator implements Cloneable {
        static DONE: number

        clone(): Object;

        abstract first(): number;

        abstract last(): number;

        abstract next(arg0: number): number;

        abstract next(): number;

        abstract previous(): number;

        abstract following(arg0: number): number;

        preceding(arg0: number): number;

        isBoundary(arg0: number): boolean;

        abstract current(): number;

        abstract getText(): CharacterIterator;

        setText(arg0: String): void;

        abstract setText(arg0: CharacterIterator): void;

        static getWordInstance(): BreakIterator;

        static getWordInstance(arg0: Locale): BreakIterator;

        static getLineInstance(): BreakIterator;

        static getLineInstance(arg0: Locale): BreakIterator;

        static getCharacterInstance(): BreakIterator;

        static getCharacterInstance(arg0: Locale): BreakIterator;

        static getSentenceInstance(): BreakIterator;

        static getSentenceInstance(arg0: Locale): BreakIterator;

        static getAvailableLocales(): Locale[];
    }

    export namespace CharacterIterator {
        const DONE: String
    }

    export interface CharacterIterator extends Cloneable {
        DONE: String

        first(): String;

        last(): String;

        current(): String;

        next(): String;

        previous(): String;

        setIndex(arg0: number): String;

        getBeginIndex(): number;

        getEndIndex(): number;

        getIndex(): number;

        clone(): Object;
    }

    export class ChoiceFormat extends NumberFormat {
        constructor(arg0: String);
        constructor(arg0: number[], arg1: String[]);

        applyPattern(arg0: String): void;

        toPattern(): String;

        setChoices(arg0: number[], arg1: String[]): void;

        getLimits(): number[];

        getFormats(): Object[];

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        parse(arg0: String, arg1: ParsePosition): Number;

        static nextDouble(arg0: number): number;

        static previousDouble(arg0: number): number;

        clone(): Object;

        hashCode(): number;

        equals(arg0: Object): boolean;

        static nextDouble(arg0: number, arg1: boolean): number;
    }

    export class CollationElementIterator {
        static NULLORDER: number

        reset(): void;

        next(): number;

        previous(): number;

        static primaryOrder(arg0: number): number;

        static secondaryOrder(arg0: number): number;

        static tertiaryOrder(arg0: number): number;

        setOffset(arg0: number): void;

        getOffset(): number;

        getMaxExpansion(arg0: number): number;

        setText(arg0: String): void;

        setText(arg0: CharacterIterator): void;
    }

    export abstract class CollationKey extends Object implements Comparable<CollationKey> {

        abstract compareTo(arg0: CollationKey): number;

        getSourceString(): String;

        abstract toByteArray(): number[];
    }

    export interface Collator extends Comparator<Object>, Cloneable { }
    export abstract class Collator extends Object implements Comparator<Object>, Cloneable {
        static PRIMARY: number
        static SECONDARY: number
        static TERTIARY: number
        static IDENTICAL: number
        static NO_DECOMPOSITION: number
        static CANONICAL_DECOMPOSITION: number
        static FULL_DECOMPOSITION: number

        static getInstance(): Collator;

        static getInstance(arg0: Locale): Collator;

        abstract compare(arg0: String, arg1: String): number;

        compare(arg0: Object, arg1: Object): number;

        abstract getCollationKey(arg0: String): CollationKey;

        equals(arg0: String, arg1: String): boolean;

        getStrength(): number;

        setStrength(arg0: number): void;

        getDecomposition(): number;

        setDecomposition(arg0: number): void;

        static getAvailableLocales(): Locale[];

        clone(): Object;

        equals(arg0: Object): boolean;

        abstract hashCode(): number;
    }

    export class CompactNumberFormat extends NumberFormat {
        constructor(arg0: String, arg1: DecimalFormatSymbols, arg2: String[]);
        constructor(arg0: String, arg1: DecimalFormatSymbols, arg2: String[], arg3: String);

        format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        formatToCharacterIterator(arg0: Object): AttributedCharacterIterator;

        parse(arg0: String, arg1: ParsePosition): Number;

        setMaximumIntegerDigits(arg0: number): void;

        setMinimumIntegerDigits(arg0: number): void;

        setMinimumFractionDigits(arg0: number): void;

        setMaximumFractionDigits(arg0: number): void;

        getRoundingMode(): RoundingMode;

        setRoundingMode(arg0: RoundingMode): void;

        getGroupingSize(): number;

        setGroupingSize(arg0: number): void;

        isGroupingUsed(): boolean;

        setGroupingUsed(arg0: boolean): void;

        isParseIntegerOnly(): boolean;

        setParseIntegerOnly(arg0: boolean): void;

        isParseBigDecimal(): boolean;

        setParseBigDecimal(arg0: boolean): void;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): CompactNumberFormat;
    }

    export abstract class DateFormat extends Format {
        static ERA_FIELD: number
        static YEAR_FIELD: number
        static MONTH_FIELD: number
        static DATE_FIELD: number
        static HOUR_OF_DAY1_FIELD: number
        static HOUR_OF_DAY0_FIELD: number
        static MINUTE_FIELD: number
        static SECOND_FIELD: number
        static MILLISECOND_FIELD: number
        static DAY_OF_WEEK_FIELD: number
        static DAY_OF_YEAR_FIELD: number
        static DAY_OF_WEEK_IN_MONTH_FIELD: number
        static WEEK_OF_YEAR_FIELD: number
        static WEEK_OF_MONTH_FIELD: number
        static AM_PM_FIELD: number
        static HOUR1_FIELD: number
        static HOUR0_FIELD: number
        static TIMEZONE_FIELD: number
        static FULL: number
        static LONG: number
        static MEDIUM: number
        static SHORT: number
        static DEFAULT: number

        format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        abstract format(arg0: Date, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: Date): String;

        parse(arg0: String): Date;

        abstract parse(arg0: String, arg1: ParsePosition): Date;

        parseObject(arg0: String, arg1: ParsePosition): Object;

        static getTimeInstance(): DateFormat;

        static getTimeInstance(arg0: number): DateFormat;

        static getTimeInstance(arg0: number, arg1: Locale): DateFormat;

        static getDateInstance(): DateFormat;

        static getDateInstance(arg0: number): DateFormat;

        static getDateInstance(arg0: number, arg1: Locale): DateFormat;

        static getDateTimeInstance(): DateFormat;

        static getDateTimeInstance(arg0: number, arg1: number): DateFormat;

        static getDateTimeInstance(arg0: number, arg1: number, arg2: Locale): DateFormat;

        static getInstance(): DateFormat;

        static getAvailableLocales(): Locale[];

        setCalendar(arg0: Calendar): void;

        getCalendar(): Calendar;

        setNumberFormat(arg0: NumberFormat): void;

        getNumberFormat(): NumberFormat;

        setTimeZone(arg0: TimeZone): void;

        getTimeZone(): TimeZone;

        setLenient(arg0: boolean): void;

        isLenient(): boolean;

        hashCode(): number;

        equals(arg0: Object): boolean;

        clone(): Object;
    }
    export namespace DateFormat {
        export class Field extends Format.Field {
            static ERA: DateFormat.Field
            static YEAR: DateFormat.Field
            static MONTH: DateFormat.Field
            static DAY_OF_MONTH: DateFormat.Field
            static HOUR_OF_DAY1: DateFormat.Field
            static HOUR_OF_DAY0: DateFormat.Field
            static MINUTE: DateFormat.Field
            static SECOND: DateFormat.Field
            static MILLISECOND: DateFormat.Field
            static DAY_OF_WEEK: DateFormat.Field
            static DAY_OF_YEAR: DateFormat.Field
            static DAY_OF_WEEK_IN_MONTH: DateFormat.Field
            static WEEK_OF_YEAR: DateFormat.Field
            static WEEK_OF_MONTH: DateFormat.Field
            static AM_PM: DateFormat.Field
            static HOUR1: DateFormat.Field
            static HOUR0: DateFormat.Field
            static TIME_ZONE: DateFormat.Field

            static ofCalendarField(arg0: number): DateFormat.Field;

            getCalendarField(): number;
        }

    }

    export class DateFormatSymbols implements Serializable, Cloneable {
        constructor();
        constructor(arg0: Locale);

        static getAvailableLocales(): Locale[];

        static getInstance(): DateFormatSymbols;

        static getInstance(arg0: Locale): DateFormatSymbols;

        getEras(): String[];

        setEras(arg0: String[]): void;

        getMonths(): String[];

        setMonths(arg0: String[]): void;

        getShortMonths(): String[];

        setShortMonths(arg0: String[]): void;

        getWeekdays(): String[];

        setWeekdays(arg0: String[]): void;

        getShortWeekdays(): String[];

        setShortWeekdays(arg0: String[]): void;

        getAmPmStrings(): String[];

        setAmPmStrings(arg0: String[]): void;

        getZoneStrings(): Array<Array<String>>;

        setZoneStrings(arg0: Array<Array<String>>): void;

        getLocalPatternChars(): String;

        setLocalPatternChars(arg0: String): void;

        clone(): Object;

        hashCode(): number;

        equals(arg0: Object): boolean;
    }

    export class DecimalFormat extends NumberFormat {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: DecimalFormatSymbols);

        format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        formatToCharacterIterator(arg0: Object): AttributedCharacterIterator;

        parse(arg0: String, arg1: ParsePosition): Number;

        getDecimalFormatSymbols(): DecimalFormatSymbols;

        setDecimalFormatSymbols(arg0: DecimalFormatSymbols): void;

        getPositivePrefix(): String;

        setPositivePrefix(arg0: String): void;

        getNegativePrefix(): String;

        setNegativePrefix(arg0: String): void;

        getPositiveSuffix(): String;

        setPositiveSuffix(arg0: String): void;

        getNegativeSuffix(): String;

        setNegativeSuffix(arg0: String): void;

        getMultiplier(): number;

        setMultiplier(arg0: number): void;

        setGroupingUsed(arg0: boolean): void;

        getGroupingSize(): number;

        setGroupingSize(arg0: number): void;

        isDecimalSeparatorAlwaysShown(): boolean;

        setDecimalSeparatorAlwaysShown(arg0: boolean): void;

        isParseBigDecimal(): boolean;

        setParseBigDecimal(arg0: boolean): void;

        clone(): Object;

        equals(arg0: Object): boolean;

        hashCode(): number;

        toPattern(): String;

        toLocalizedPattern(): String;

        applyPattern(arg0: String): void;

        applyLocalizedPattern(arg0: String): void;

        setMaximumIntegerDigits(arg0: number): void;

        setMinimumIntegerDigits(arg0: number): void;

        setMaximumFractionDigits(arg0: number): void;

        setMinimumFractionDigits(arg0: number): void;

        getMaximumIntegerDigits(): number;

        getMinimumIntegerDigits(): number;

        getMaximumFractionDigits(): number;

        getMinimumFractionDigits(): number;

        getCurrency(): Currency;

        setCurrency(arg0: Currency): void;

        getRoundingMode(): RoundingMode;

        setRoundingMode(arg0: RoundingMode): void;
    }

    export class DecimalFormatSymbols implements Cloneable, Serializable {
        constructor();
        constructor(arg0: Locale);

        static getAvailableLocales(): Locale[];

        static getInstance(): DecimalFormatSymbols;

        static getInstance(arg0: Locale): DecimalFormatSymbols;

        getZeroDigit(): String;

        setZeroDigit(arg0: String): void;

        getGroupingSeparator(): String;

        setGroupingSeparator(arg0: String): void;

        getDecimalSeparator(): String;

        setDecimalSeparator(arg0: String): void;

        getPerMill(): String;

        setPerMill(arg0: String): void;

        getPercent(): String;

        setPercent(arg0: String): void;

        getDigit(): String;

        setDigit(arg0: String): void;

        getPatternSeparator(): String;

        setPatternSeparator(arg0: String): void;

        getInfinity(): String;

        setInfinity(arg0: String): void;

        getNaN(): String;

        setNaN(arg0: String): void;

        getMinusSign(): String;

        setMinusSign(arg0: String): void;

        getCurrencySymbol(): String;

        setCurrencySymbol(arg0: String): void;

        getInternationalCurrencySymbol(): String;

        setInternationalCurrencySymbol(arg0: String): void;

        getCurrency(): Currency;

        setCurrency(arg0: Currency): void;

        getMonetaryDecimalSeparator(): String;

        setMonetaryDecimalSeparator(arg0: String): void;

        getExponentSeparator(): String;

        setExponentSeparator(arg0: String): void;

        getMonetaryGroupingSeparator(): String;

        setMonetaryGroupingSeparator(arg0: String): void;

        clone(): Object;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class FieldPosition {
        constructor(arg0: number);
        constructor(arg0: Format.Field);
        constructor(arg0: Format.Field, arg1: number);

        getFieldAttribute(): Format.Field;

        getField(): number;

        getBeginIndex(): number;

        getEndIndex(): number;

        setBeginIndex(arg0: number): void;

        setEndIndex(arg0: number): void;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export abstract class Format implements Serializable, Cloneable {

        format(arg0: Object): String;

        abstract format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        formatToCharacterIterator(arg0: Object): AttributedCharacterIterator;

        abstract parseObject(arg0: String, arg1: ParsePosition): Object;

        parseObject(arg0: String): Object;

        clone(): Object;
    }
    export namespace Format {
        export class Field extends AttributedCharacterIterator.Attribute {
        }

    }

    export class MessageFormat extends Format {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Locale);

        setLocale(arg0: Locale): void;

        getLocale(): Locale;

        applyPattern(arg0: String): void;

        toPattern(): String;

        setFormatsByArgumentIndex(arg0: Format[]): void;

        setFormats(arg0: Format[]): void;

        setFormatByArgumentIndex(arg0: number, arg1: Format): void;

        setFormat(arg0: number, arg1: Format): void;

        getFormatsByArgumentIndex(): Format[];

        getFormats(): Format[];

        format(arg0: Object[], arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        static format(arg0: String, arg1: Object[]): String;

        format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        formatToCharacterIterator(arg0: Object): AttributedCharacterIterator;

        parse(arg0: String, arg1: ParsePosition): Object[];

        parse(arg0: String): Object[];

        parseObject(arg0: String, arg1: ParsePosition): Object;

        clone(): Object;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }
    export namespace MessageFormat {
        export class Field extends Format.Field {
            static ARGUMENT: MessageFormat.Field
        }

    }

    export class Normalizer {

        static normalize(arg0: CharSequence, arg1: Normalizer.Form): String;

        static isNormalized(arg0: CharSequence, arg1: Normalizer.Form): boolean;
    }
    export namespace Normalizer {
        export class Form extends Enum<Normalizer.Form> {
            static NFD: Normalizer.Form
            static NFC: Normalizer.Form
            static NFKD: Normalizer.Form
            static NFKC: Normalizer.Form

            static values(): Normalizer.Form[];

            static valueOf(arg0: String): Normalizer.Form;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export abstract class NumberFormat extends Format {
        static INTEGER_FIELD: number
        static FRACTION_FIELD: number

        format(arg0: Object, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        parseObject(arg0: String, arg1: ParsePosition): Object;

        format(arg0: number): String;

        format(arg0: number): String;

        abstract format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        abstract format(arg0: number, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        abstract parse(arg0: String, arg1: ParsePosition): Number;

        parse(arg0: String): Number;

        isParseIntegerOnly(): boolean;

        setParseIntegerOnly(arg0: boolean): void;

        static getInstance(): NumberFormat;

        static getInstance(arg0: Locale): NumberFormat;

        static getNumberInstance(): NumberFormat;

        static getNumberInstance(arg0: Locale): NumberFormat;

        static getIntegerInstance(): NumberFormat;

        static getIntegerInstance(arg0: Locale): NumberFormat;

        static getCurrencyInstance(): NumberFormat;

        static getCurrencyInstance(arg0: Locale): NumberFormat;

        static getPercentInstance(): NumberFormat;

        static getPercentInstance(arg0: Locale): NumberFormat;

        static getCompactNumberInstance(): NumberFormat;

        static getCompactNumberInstance(arg0: Locale, arg1: NumberFormat.Style): NumberFormat;

        static getAvailableLocales(): Locale[];

        hashCode(): number;

        equals(arg0: Object): boolean;

        clone(): Object;

        isGroupingUsed(): boolean;

        setGroupingUsed(arg0: boolean): void;

        getMaximumIntegerDigits(): number;

        setMaximumIntegerDigits(arg0: number): void;

        getMinimumIntegerDigits(): number;

        setMinimumIntegerDigits(arg0: number): void;

        getMaximumFractionDigits(): number;

        setMaximumFractionDigits(arg0: number): void;

        getMinimumFractionDigits(): number;

        setMinimumFractionDigits(arg0: number): void;

        getCurrency(): Currency;

        setCurrency(arg0: Currency): void;

        getRoundingMode(): RoundingMode;

        setRoundingMode(arg0: RoundingMode): void;
    }
    export namespace NumberFormat {
        export class Field extends Format.Field {
            static INTEGER: NumberFormat.Field
            static FRACTION: NumberFormat.Field
            static EXPONENT: NumberFormat.Field
            static DECIMAL_SEPARATOR: NumberFormat.Field
            static SIGN: NumberFormat.Field
            static GROUPING_SEPARATOR: NumberFormat.Field
            static EXPONENT_SYMBOL: NumberFormat.Field
            static PERCENT: NumberFormat.Field
            static PERMILLE: NumberFormat.Field
            static CURRENCY: NumberFormat.Field
            static EXPONENT_SIGN: NumberFormat.Field
            static PREFIX: NumberFormat.Field
            static SUFFIX: NumberFormat.Field
        }

        export class Style extends Enum<NumberFormat.Style> {
            static SHORT: NumberFormat.Style
            static LONG: NumberFormat.Style

            static values(): NumberFormat.Style[];

            static valueOf(arg0: String): NumberFormat.Style;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class ParseException extends Exception {
        constructor(arg0: String, arg1: number);

        getErrorOffset(): number;
    }

    export class ParsePosition {
        constructor(arg0: number);

        getIndex(): number;

        setIndex(arg0: number): void;

        setErrorIndex(arg0: number): void;

        getErrorIndex(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface RuleBasedCollator { }
    export class RuleBasedCollator extends Collator {
        constructor(arg0: String);

        getRules(): String;

        getCollationElementIterator(arg0: String): CollationElementIterator;

        getCollationElementIterator(arg0: CharacterIterator): CollationElementIterator;

        compare(arg0: String, arg1: String): number;

        getCollationKey(arg0: String): CollationKey;

        clone(): Object;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class SimpleDateFormat extends DateFormat {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Locale);
        constructor(arg0: String, arg1: DateFormatSymbols);

        set2DigitYearStart(arg0: Date): void;

        get2DigitYearStart(): Date;

        format(arg0: Date, arg1: StringBuffer, arg2: FieldPosition): StringBuffer;

        formatToCharacterIterator(arg0: Object): AttributedCharacterIterator;

        parse(arg0: String, arg1: ParsePosition): Date;

        toPattern(): String;

        toLocalizedPattern(): String;

        applyPattern(arg0: String): void;

        applyLocalizedPattern(arg0: String): void;

        getDateFormatSymbols(): DateFormatSymbols;

        setDateFormatSymbols(arg0: DateFormatSymbols): void;

        clone(): Object;

        hashCode(): number;

        equals(arg0: Object): boolean;
    }

    export class StringCharacterIterator implements CharacterIterator {
        constructor(arg0: String);
        constructor(arg0: String, arg1: number);
        constructor(arg0: String, arg1: number, arg2: number, arg3: number);

        setText(arg0: String): void;

        first(): String;

        last(): String;

        setIndex(arg0: number): String;

        current(): String;

        next(): String;

        previous(): String;

        getBeginIndex(): number;

        getEndIndex(): number;

        getIndex(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): Object;
    }

}
/// <reference path="java.util.logging.d.ts" />
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.math.d.ts" />
/// <reference path="javax.xml.transform.d.ts" />
declare module '@java/java.sql' {
    import { Logger } from '@java/java.util.logging'
    import { BasicPermission } from '@java/java.security'
    import { Enum, Integer, Iterable, AutoCloseable, Throwable, Class, String, Exception } from '@java/java.lang'
    import { Iterator, Enumeration, Properties, Map, Date, Calendar } from '@java/java.util'
    import { LocalDateTime, LocalTime, LocalDate, Instant } from '@java/java.time'
    import { URL } from '@java/java.net'
    import { Reader, PrintStream, InputStream, Writer, OutputStream, PrintWriter } from '@java/java.io'
    import { Executor } from '@java/java.util.concurrent'
    import { Stream } from '@java/java.util.stream'
    import { BigDecimal } from '@java/java.math'
    import { Source, Result } from '@java/javax.xml.transform'
    export interface Array {

        getBaseTypeName(): String;

        getBaseType(): number;

        getArray(): Object;

        getArray(arg0: Map<String, Class<any>>): Object;

        getArray(arg0: number, arg1: number): Object;

        getArray(arg0: number, arg1: number, arg2: Map<String, Class<any>>): Object;

        getResultSet(): ResultSet;

        getResultSet(arg0: Map<String, Class<any>>): ResultSet;

        getResultSet(arg0: number, arg1: number): ResultSet;

        getResultSet(arg0: number, arg1: number, arg2: Map<String, Class<any>>): ResultSet;

        free(): void;
    }

    export interface BatchUpdateException { }
    export class BatchUpdateException extends SQLException {
        constructor(arg0: String, arg1: String, arg2: number, arg3: number[]);
        constructor(arg0: String, arg1: String, arg2: number[]);
        constructor(arg0: String, arg1: number[]);
        constructor(arg0: number[]);
        constructor();
        constructor(arg0: Throwable);
        constructor(arg0: number[], arg1: Throwable);
        constructor(arg0: String, arg1: number[], arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number[], arg3: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: number[], arg4: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: number[], arg4: Throwable);

        getUpdateCounts(): number[];

        getLargeUpdateCounts(): number[];
    }

    export interface Blob {

        length(): number;

        getBytes(arg0: number, arg1: number): number[];

        getBinaryStream(): InputStream;

        position(arg0: number[], arg1: number): number;

        position(arg0: Blob, arg1: number): number;

        setBytes(arg0: number, arg1: number[]): number;

        setBytes(arg0: number, arg1: number[], arg2: number, arg3: number): number;

        setBinaryStream(arg0: number): OutputStream;

        truncate(arg0: number): void;

        free(): void;

        getBinaryStream(arg0: number, arg1: number): InputStream;
    }

    export interface CallableStatement extends PreparedStatement {

        registerOutParameter(arg0: number, arg1: number): void;

        registerOutParameter(arg0: number, arg1: number, arg2: number): void;

        wasNull(): boolean;

        getString(arg0: number): String;

        getBoolean(arg0: number): boolean;

        getByte(arg0: number): number;

        getShort(arg0: number): number;

        getInt(arg0: number): number;

        getLong(arg0: number): number;

        getFloat(arg0: number): number;

        getDouble(arg0: number): number;

        getBigDecimal(arg0: number, arg1: number): BigDecimal;

        getBytes(arg0: number): number[];

        getDate(arg0: number): Date;

        getTime(arg0: number): Time;

        getTimestamp(arg0: number): Timestamp;

        getObject(arg0: number): Object;

        getBigDecimal(arg0: number): BigDecimal;

        getObject(arg0: number, arg1: Map<String, Class<any>>): Object;

        getRef(arg0: number): Ref;

        getBlob(arg0: number): Blob;

        getClob(arg0: number): Clob;

        getArray(arg0: number): Array;

        getDate(arg0: number, arg1: Calendar): Date;

        getTime(arg0: number, arg1: Calendar): Time;

        getTimestamp(arg0: number, arg1: Calendar): Timestamp;

        registerOutParameter(arg0: number, arg1: number, arg2: String): void;

        registerOutParameter(arg0: String, arg1: number): void;

        registerOutParameter(arg0: String, arg1: number, arg2: number): void;

        registerOutParameter(arg0: String, arg1: number, arg2: String): void;

        getURL(arg0: number): URL;

        setURL(arg0: String, arg1: URL): void;

        setNull(arg0: String, arg1: number): void;

        setBoolean(arg0: String, arg1: boolean): void;

        setByte(arg0: String, arg1: number): void;

        setShort(arg0: String, arg1: number): void;

        setInt(arg0: String, arg1: number): void;

        setLong(arg0: String, arg1: number): void;

        setFloat(arg0: String, arg1: number): void;

        setDouble(arg0: String, arg1: number): void;

        setBigDecimal(arg0: String, arg1: BigDecimal): void;

        setString(arg0: String, arg1: String): void;

        setBytes(arg0: String, arg1: number[]): void;

        setDate(arg0: String, arg1: Date): void;

        setTime(arg0: String, arg1: Time): void;

        setTimestamp(arg0: String, arg1: Timestamp): void;

        setAsciiStream(arg0: String, arg1: InputStream, arg2: number): void;

        setBinaryStream(arg0: String, arg1: InputStream, arg2: number): void;

        setObject(arg0: String, arg1: Object, arg2: number, arg3: number): void;

        setObject(arg0: String, arg1: Object, arg2: number): void;

        setObject(arg0: String, arg1: Object): void;

        setCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        setDate(arg0: String, arg1: Date, arg2: Calendar): void;

        setTime(arg0: String, arg1: Time, arg2: Calendar): void;

        setTimestamp(arg0: String, arg1: Timestamp, arg2: Calendar): void;

        setNull(arg0: String, arg1: number, arg2: String): void;

        getString(arg0: String): String;

        getBoolean(arg0: String): boolean;

        getByte(arg0: String): number;

        getShort(arg0: String): number;

        getInt(arg0: String): number;

        getLong(arg0: String): number;

        getFloat(arg0: String): number;

        getDouble(arg0: String): number;

        getBytes(arg0: String): number[];

        getDate(arg0: String): Date;

        getTime(arg0: String): Time;

        getTimestamp(arg0: String): Timestamp;

        getObject(arg0: String): Object;

        getBigDecimal(arg0: String): BigDecimal;

        getObject(arg0: String, arg1: Map<String, Class<any>>): Object;

        getRef(arg0: String): Ref;

        getBlob(arg0: String): Blob;

        getClob(arg0: String): Clob;

        getArray(arg0: String): Array;

        getDate(arg0: String, arg1: Calendar): Date;

        getTime(arg0: String, arg1: Calendar): Time;

        getTimestamp(arg0: String, arg1: Calendar): Timestamp;

        getURL(arg0: String): URL;

        getRowId(arg0: number): RowId;

        getRowId(arg0: String): RowId;

        setRowId(arg0: String, arg1: RowId): void;

        setNString(arg0: String, arg1: String): void;

        setNCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        setNClob(arg0: String, arg1: NClob): void;

        setClob(arg0: String, arg1: Reader, arg2: number): void;

        setBlob(arg0: String, arg1: InputStream, arg2: number): void;

        setNClob(arg0: String, arg1: Reader, arg2: number): void;

        getNClob(arg0: number): NClob;

        getNClob(arg0: String): NClob;

        setSQLXML(arg0: String, arg1: SQLXML): void;

        getSQLXML(arg0: number): SQLXML;

        getSQLXML(arg0: String): SQLXML;

        getNString(arg0: number): String;

        getNString(arg0: String): String;

        getNCharacterStream(arg0: number): Reader;

        getNCharacterStream(arg0: String): Reader;

        getCharacterStream(arg0: number): Reader;

        getCharacterStream(arg0: String): Reader;

        setBlob(arg0: String, arg1: Blob): void;

        setClob(arg0: String, arg1: Clob): void;

        setAsciiStream(arg0: String, arg1: InputStream, arg2: number): void;

        setBinaryStream(arg0: String, arg1: InputStream, arg2: number): void;

        setCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        setAsciiStream(arg0: String, arg1: InputStream): void;

        setBinaryStream(arg0: String, arg1: InputStream): void;

        setCharacterStream(arg0: String, arg1: Reader): void;

        setNCharacterStream(arg0: String, arg1: Reader): void;

        setClob(arg0: String, arg1: Reader): void;

        setBlob(arg0: String, arg1: InputStream): void;

        setNClob(arg0: String, arg1: Reader): void;

        getObject<T extends Object>(arg0: number, arg1: Class<T>): T;

        getObject<T extends Object>(arg0: String, arg1: Class<T>): T;

/* default */ setObject(arg0: String, arg1: Object, arg2: SQLType, arg3: number): void;

/* default */ setObject(arg0: String, arg1: Object, arg2: SQLType): void;

/* default */ registerOutParameter(arg0: number, arg1: SQLType): void;

/* default */ registerOutParameter(arg0: number, arg1: SQLType, arg2: number): void;

/* default */ registerOutParameter(arg0: number, arg1: SQLType, arg2: String): void;

/* default */ registerOutParameter(arg0: String, arg1: SQLType): void;

/* default */ registerOutParameter(arg0: String, arg1: SQLType, arg2: number): void;

/* default */ registerOutParameter(arg0: String, arg1: SQLType, arg2: String): void;
    }

    export class ClientInfoStatus extends Enum<ClientInfoStatus> {
        static REASON_UNKNOWN: ClientInfoStatus
        static REASON_UNKNOWN_PROPERTY: ClientInfoStatus
        static REASON_VALUE_INVALID: ClientInfoStatus
        static REASON_VALUE_TRUNCATED: ClientInfoStatus

        static values(): ClientInfoStatus[];

        static valueOf(arg0: String): ClientInfoStatus;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface Clob {

        length(): number;

        getSubString(arg0: number, arg1: number): String;

        getCharacterStream(): Reader;

        getAsciiStream(): InputStream;

        position(arg0: String, arg1: number): number;

        position(arg0: Clob, arg1: number): number;

        setString(arg0: number, arg1: String): number;

        setString(arg0: number, arg1: String, arg2: number, arg3: number): number;

        setAsciiStream(arg0: number): OutputStream;

        setCharacterStream(arg0: number): Writer;

        truncate(arg0: number): void;

        free(): void;

        getCharacterStream(arg0: number, arg1: number): Reader;
    }

    export namespace Connection {
        const TRANSACTION_NONE: number
        const TRANSACTION_READ_UNCOMMITTED: number
        const TRANSACTION_READ_COMMITTED: number
        const TRANSACTION_REPEATABLE_READ: number
        const TRANSACTION_SERIALIZABLE: number
    }

    export interface Connection extends Wrapper, AutoCloseable {
        TRANSACTION_NONE: number
        TRANSACTION_READ_UNCOMMITTED: number
        TRANSACTION_READ_COMMITTED: number
        TRANSACTION_REPEATABLE_READ: number
        TRANSACTION_SERIALIZABLE: number

        createStatement(): Statement;

        prepareStatement(arg0: String): PreparedStatement;

        prepareCall(arg0: String): CallableStatement;

        nativeSQL(arg0: String): String;

        setAutoCommit(arg0: boolean): void;

        getAutoCommit(): boolean;

        commit(): void;

        rollback(): void;

        close(): void;

        isClosed(): boolean;

        getMetaData(): DatabaseMetaData;

        setReadOnly(arg0: boolean): void;

        isReadOnly(): boolean;

        setCatalog(arg0: String): void;

        getCatalog(): String;

        setTransactionIsolation(arg0: number): void;

        getTransactionIsolation(): number;

        getWarnings(): SQLWarning;

        clearWarnings(): void;

        createStatement(arg0: number, arg1: number): Statement;

        prepareStatement(arg0: String, arg1: number, arg2: number): PreparedStatement;

        prepareCall(arg0: String, arg1: number, arg2: number): CallableStatement;

        getTypeMap(): Map<String, Class<any>>;

        setTypeMap(arg0: Map<String, Class<any>>): void;

        setHoldability(arg0: number): void;

        getHoldability(): number;

        setSavepoint(): Savepoint;

        setSavepoint(arg0: String): Savepoint;

        rollback(arg0: Savepoint): void;

        releaseSavepoint(arg0: Savepoint): void;

        createStatement(arg0: number, arg1: number, arg2: number): Statement;

        prepareStatement(arg0: String, arg1: number, arg2: number, arg3: number): PreparedStatement;

        prepareCall(arg0: String, arg1: number, arg2: number, arg3: number): CallableStatement;

        prepareStatement(arg0: String, arg1: number): PreparedStatement;

        prepareStatement(arg0: String, arg1: number[]): PreparedStatement;

        prepareStatement(arg0: String, arg1: String[]): PreparedStatement;

        createClob(): Clob;

        createBlob(): Blob;

        createNClob(): NClob;

        createSQLXML(): SQLXML;

        isValid(arg0: number): boolean;

        setClientInfo(arg0: String, arg1: String): void;

        setClientInfo(arg0: Properties): void;

        getClientInfo(arg0: String): String;

        getClientInfo(): Properties;

        createArrayOf(arg0: String, arg1: Object[]): Array;

        createStruct(arg0: String, arg1: Object[]): Struct;

        setSchema(arg0: String): void;

        getSchema(): String;

        abort(arg0: Executor): void;

        setNetworkTimeout(arg0: Executor, arg1: number): void;

        getNetworkTimeout(): number;

/* default */ beginRequest(): void;

/* default */ endRequest(): void;

/* default */ setShardingKeyIfValid(arg0: ShardingKey, arg1: ShardingKey, arg2: number): boolean;

/* default */ setShardingKeyIfValid(arg0: ShardingKey, arg1: number): boolean;

/* default */ setShardingKey(arg0: ShardingKey, arg1: ShardingKey): void;

/* default */ setShardingKey(arg0: ShardingKey): void;
    }

    export interface ConnectionBuilder {

        user(arg0: String): ConnectionBuilder;

        password(arg0: String): ConnectionBuilder;

        shardingKey(arg0: ShardingKey): ConnectionBuilder;

        superShardingKey(arg0: ShardingKey): ConnectionBuilder;

        build(): Connection;
    }

    export interface DataTruncation { }
    export class DataTruncation extends SQLWarning {
        constructor(arg0: number, arg1: boolean, arg2: boolean, arg3: number, arg4: number);
        constructor(arg0: number, arg1: boolean, arg2: boolean, arg3: number, arg4: number, arg5: Throwable);

        getIndex(): number;

        getParameter(): boolean;

        getRead(): boolean;

        getDataSize(): number;

        getTransferSize(): number;
    }

    export namespace DatabaseMetaData {
        const procedureResultUnknown: number
        const procedureNoResult: number
        const procedureReturnsResult: number
        const procedureColumnUnknown: number
        const procedureColumnIn: number
        const procedureColumnInOut: number
        const procedureColumnOut: number
        const procedureColumnReturn: number
        const procedureColumnResult: number
        const procedureNoNulls: number
        const procedureNullable: number
        const procedureNullableUnknown: number
        const columnNoNulls: number
        const columnNullable: number
        const columnNullableUnknown: number
        const bestRowTemporary: number
        const bestRowTransaction: number
        const bestRowSession: number
        const bestRowUnknown: number
        const bestRowNotPseudo: number
        const bestRowPseudo: number
        const versionColumnUnknown: number
        const versionColumnNotPseudo: number
        const versionColumnPseudo: number
        const importedKeyCascade: number
        const importedKeyRestrict: number
        const importedKeySetNull: number
        const importedKeyNoAction: number
        const importedKeySetDefault: number
        const importedKeyInitiallyDeferred: number
        const importedKeyInitiallyImmediate: number
        const importedKeyNotDeferrable: number
        const typeNoNulls: number
        const typeNullable: number
        const typeNullableUnknown: number
        const typePredNone: number
        const typePredChar: number
        const typePredBasic: number
        const typeSearchable: number
        const tableIndexStatistic: number
        const tableIndexClustered: number
        const tableIndexHashed: number
        const tableIndexOther: number
        const attributeNoNulls: number
        const attributeNullable: number
        const attributeNullableUnknown: number
        const sqlStateXOpen: number
        const sqlStateSQL: number
        const sqlStateSQL99: number
        const functionColumnUnknown: number
        const functionColumnIn: number
        const functionColumnInOut: number
        const functionColumnOut: number
        const functionReturn: number
        const functionColumnResult: number
        const functionNoNulls: number
        const functionNullable: number
        const functionNullableUnknown: number
        const functionResultUnknown: number
        const functionNoTable: number
        const functionReturnsTable: number
    }

    export interface DatabaseMetaData extends Wrapper {
        procedureResultUnknown: number
        procedureNoResult: number
        procedureReturnsResult: number
        procedureColumnUnknown: number
        procedureColumnIn: number
        procedureColumnInOut: number
        procedureColumnOut: number
        procedureColumnReturn: number
        procedureColumnResult: number
        procedureNoNulls: number
        procedureNullable: number
        procedureNullableUnknown: number
        columnNoNulls: number
        columnNullable: number
        columnNullableUnknown: number
        bestRowTemporary: number
        bestRowTransaction: number
        bestRowSession: number
        bestRowUnknown: number
        bestRowNotPseudo: number
        bestRowPseudo: number
        versionColumnUnknown: number
        versionColumnNotPseudo: number
        versionColumnPseudo: number
        importedKeyCascade: number
        importedKeyRestrict: number
        importedKeySetNull: number
        importedKeyNoAction: number
        importedKeySetDefault: number
        importedKeyInitiallyDeferred: number
        importedKeyInitiallyImmediate: number
        importedKeyNotDeferrable: number
        typeNoNulls: number
        typeNullable: number
        typeNullableUnknown: number
        typePredNone: number
        typePredChar: number
        typePredBasic: number
        typeSearchable: number
        tableIndexStatistic: number
        tableIndexClustered: number
        tableIndexHashed: number
        tableIndexOther: number
        attributeNoNulls: number
        attributeNullable: number
        attributeNullableUnknown: number
        sqlStateXOpen: number
        sqlStateSQL: number
        sqlStateSQL99: number
        functionColumnUnknown: number
        functionColumnIn: number
        functionColumnInOut: number
        functionColumnOut: number
        functionReturn: number
        functionColumnResult: number
        functionNoNulls: number
        functionNullable: number
        functionNullableUnknown: number
        functionResultUnknown: number
        functionNoTable: number
        functionReturnsTable: number

        allProceduresAreCallable(): boolean;

        allTablesAreSelectable(): boolean;

        getURL(): String;

        getUserName(): String;

        isReadOnly(): boolean;

        nullsAreSortedHigh(): boolean;

        nullsAreSortedLow(): boolean;

        nullsAreSortedAtStart(): boolean;

        nullsAreSortedAtEnd(): boolean;

        getDatabaseProductName(): String;

        getDatabaseProductVersion(): String;

        getDriverName(): String;

        getDriverVersion(): String;

        getDriverMajorVersion(): number;

        getDriverMinorVersion(): number;

        usesLocalFiles(): boolean;

        usesLocalFilePerTable(): boolean;

        supportsMixedCaseIdentifiers(): boolean;

        storesUpperCaseIdentifiers(): boolean;

        storesLowerCaseIdentifiers(): boolean;

        storesMixedCaseIdentifiers(): boolean;

        supportsMixedCaseQuotedIdentifiers(): boolean;

        storesUpperCaseQuotedIdentifiers(): boolean;

        storesLowerCaseQuotedIdentifiers(): boolean;

        storesMixedCaseQuotedIdentifiers(): boolean;

        getIdentifierQuoteString(): String;

        getSQLKeywords(): String;

        getNumericFunctions(): String;

        getStringFunctions(): String;

        getSystemFunctions(): String;

        getTimeDateFunctions(): String;

        getSearchStringEscape(): String;

        getExtraNameCharacters(): String;

        supportsAlterTableWithAddColumn(): boolean;

        supportsAlterTableWithDropColumn(): boolean;

        supportsColumnAliasing(): boolean;

        nullPlusNonNullIsNull(): boolean;

        supportsConvert(): boolean;

        supportsConvert(arg0: number, arg1: number): boolean;

        supportsTableCorrelationNames(): boolean;

        supportsDifferentTableCorrelationNames(): boolean;

        supportsExpressionsInOrderBy(): boolean;

        supportsOrderByUnrelated(): boolean;

        supportsGroupBy(): boolean;

        supportsGroupByUnrelated(): boolean;

        supportsGroupByBeyondSelect(): boolean;

        supportsLikeEscapeClause(): boolean;

        supportsMultipleResultSets(): boolean;

        supportsMultipleTransactions(): boolean;

        supportsNonNullableColumns(): boolean;

        supportsMinimumSQLGrammar(): boolean;

        supportsCoreSQLGrammar(): boolean;

        supportsExtendedSQLGrammar(): boolean;

        supportsANSI92EntryLevelSQL(): boolean;

        supportsANSI92IntermediateSQL(): boolean;

        supportsANSI92FullSQL(): boolean;

        supportsIntegrityEnhancementFacility(): boolean;

        supportsOuterJoins(): boolean;

        supportsFullOuterJoins(): boolean;

        supportsLimitedOuterJoins(): boolean;

        getSchemaTerm(): String;

        getProcedureTerm(): String;

        getCatalogTerm(): String;

        isCatalogAtStart(): boolean;

        getCatalogSeparator(): String;

        supportsSchemasInDataManipulation(): boolean;

        supportsSchemasInProcedureCalls(): boolean;

        supportsSchemasInTableDefinitions(): boolean;

        supportsSchemasInIndexDefinitions(): boolean;

        supportsSchemasInPrivilegeDefinitions(): boolean;

        supportsCatalogsInDataManipulation(): boolean;

        supportsCatalogsInProcedureCalls(): boolean;

        supportsCatalogsInTableDefinitions(): boolean;

        supportsCatalogsInIndexDefinitions(): boolean;

        supportsCatalogsInPrivilegeDefinitions(): boolean;

        supportsPositionedDelete(): boolean;

        supportsPositionedUpdate(): boolean;

        supportsSelectForUpdate(): boolean;

        supportsStoredProcedures(): boolean;

        supportsSubqueriesInComparisons(): boolean;

        supportsSubqueriesInExists(): boolean;

        supportsSubqueriesInIns(): boolean;

        supportsSubqueriesInQuantifieds(): boolean;

        supportsCorrelatedSubqueries(): boolean;

        supportsUnion(): boolean;

        supportsUnionAll(): boolean;

        supportsOpenCursorsAcrossCommit(): boolean;

        supportsOpenCursorsAcrossRollback(): boolean;

        supportsOpenStatementsAcrossCommit(): boolean;

        supportsOpenStatementsAcrossRollback(): boolean;

        getMaxBinaryLiteralLength(): number;

        getMaxCharLiteralLength(): number;

        getMaxColumnNameLength(): number;

        getMaxColumnsInGroupBy(): number;

        getMaxColumnsInIndex(): number;

        getMaxColumnsInOrderBy(): number;

        getMaxColumnsInSelect(): number;

        getMaxColumnsInTable(): number;

        getMaxConnections(): number;

        getMaxCursorNameLength(): number;

        getMaxIndexLength(): number;

        getMaxSchemaNameLength(): number;

        getMaxProcedureNameLength(): number;

        getMaxCatalogNameLength(): number;

        getMaxRowSize(): number;

        doesMaxRowSizeIncludeBlobs(): boolean;

        getMaxStatementLength(): number;

        getMaxStatements(): number;

        getMaxTableNameLength(): number;

        getMaxTablesInSelect(): number;

        getMaxUserNameLength(): number;

        getDefaultTransactionIsolation(): number;

        supportsTransactions(): boolean;

        supportsTransactionIsolationLevel(arg0: number): boolean;

        supportsDataDefinitionAndDataManipulationTransactions(): boolean;

        supportsDataManipulationTransactionsOnly(): boolean;

        dataDefinitionCausesTransactionCommit(): boolean;

        dataDefinitionIgnoredInTransactions(): boolean;

        getProcedures(arg0: String, arg1: String, arg2: String): ResultSet;

        getProcedureColumns(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        getTables(arg0: String, arg1: String, arg2: String, arg3: String[]): ResultSet;

        getSchemas(): ResultSet;

        getCatalogs(): ResultSet;

        getTableTypes(): ResultSet;

        getColumns(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        getColumnPrivileges(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        getTablePrivileges(arg0: String, arg1: String, arg2: String): ResultSet;

        getBestRowIdentifier(arg0: String, arg1: String, arg2: String, arg3: number, arg4: boolean): ResultSet;

        getVersionColumns(arg0: String, arg1: String, arg2: String): ResultSet;

        getPrimaryKeys(arg0: String, arg1: String, arg2: String): ResultSet;

        getImportedKeys(arg0: String, arg1: String, arg2: String): ResultSet;

        getExportedKeys(arg0: String, arg1: String, arg2: String): ResultSet;

        getCrossReference(arg0: String, arg1: String, arg2: String, arg3: String, arg4: String, arg5: String): ResultSet;

        getTypeInfo(): ResultSet;

        getIndexInfo(arg0: String, arg1: String, arg2: String, arg3: boolean, arg4: boolean): ResultSet;

        supportsResultSetType(arg0: number): boolean;

        supportsResultSetConcurrency(arg0: number, arg1: number): boolean;

        ownUpdatesAreVisible(arg0: number): boolean;

        ownDeletesAreVisible(arg0: number): boolean;

        ownInsertsAreVisible(arg0: number): boolean;

        othersUpdatesAreVisible(arg0: number): boolean;

        othersDeletesAreVisible(arg0: number): boolean;

        othersInsertsAreVisible(arg0: number): boolean;

        updatesAreDetected(arg0: number): boolean;

        deletesAreDetected(arg0: number): boolean;

        insertsAreDetected(arg0: number): boolean;

        supportsBatchUpdates(): boolean;

        getUDTs(arg0: String, arg1: String, arg2: String, arg3: number[]): ResultSet;

        getConnection(): Connection;

        supportsSavepoints(): boolean;

        supportsNamedParameters(): boolean;

        supportsMultipleOpenResults(): boolean;

        supportsGetGeneratedKeys(): boolean;

        getSuperTypes(arg0: String, arg1: String, arg2: String): ResultSet;

        getSuperTables(arg0: String, arg1: String, arg2: String): ResultSet;

        getAttributes(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        supportsResultSetHoldability(arg0: number): boolean;

        getResultSetHoldability(): number;

        getDatabaseMajorVersion(): number;

        getDatabaseMinorVersion(): number;

        getJDBCMajorVersion(): number;

        getJDBCMinorVersion(): number;

        getSQLStateType(): number;

        locatorsUpdateCopy(): boolean;

        supportsStatementPooling(): boolean;

        getRowIdLifetime(): RowIdLifetime;

        getSchemas(arg0: String, arg1: String): ResultSet;

        supportsStoredFunctionsUsingCallSyntax(): boolean;

        autoCommitFailureClosesAllResultSets(): boolean;

        getClientInfoProperties(): ResultSet;

        getFunctions(arg0: String, arg1: String, arg2: String): ResultSet;

        getFunctionColumns(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        getPseudoColumns(arg0: String, arg1: String, arg2: String, arg3: String): ResultSet;

        generatedKeyAlwaysReturned(): boolean;

/* default */ getMaxLogicalLobSize(): number;

/* default */ supportsRefCursors(): boolean;

/* default */ supportsSharding(): boolean;
    }

    export class Date extends Date {
        constructor(arg0: number, arg1: number, arg2: number);
        constructor(arg0: number);

        setTime(arg0: number): void;

        static valueOf(arg0: String): Date;
        toString(): string;

        getHours(): number;

        getMinutes(): number;

        getSeconds(): number;

        setHours(arg0: number): void;

        setMinutes(arg0: number): void;

        setSeconds(arg0: number): void;

        static valueOf(arg0: LocalDate): Date;

        toLocalDate(): LocalDate;

        toInstant(): Instant;
    }

    export interface Driver {

        connect(arg0: String, arg1: Properties): Connection;

        acceptsURL(arg0: String): boolean;

        getPropertyInfo(arg0: String, arg1: Properties): DriverPropertyInfo[];

        getMajorVersion(): number;

        getMinorVersion(): number;

        jdbcCompliant(): boolean;

        getParentLogger(): Logger;
    }

    export interface DriverAction {

        deregister(): void;
    }

    export class DriverManager {

        static getLogWriter(): PrintWriter;

        static setLogWriter(arg0: PrintWriter): void;

        static getConnection(arg0: String, arg1: Properties): Connection;

        static getConnection(arg0: String, arg1: String, arg2: String): Connection;

        static getConnection(arg0: String): Connection;

        static getDriver(arg0: String): Driver;

        static registerDriver(arg0: Driver): void;

        static registerDriver(arg0: Driver, arg1: DriverAction): void;

        static deregisterDriver(arg0: Driver): void;

        static getDrivers(): Enumeration<Driver>;

        static drivers(): Stream<Driver>;

        static setLoginTimeout(arg0: number): void;

        static getLoginTimeout(): number;

        static setLogStream(arg0: PrintStream): void;

        static getLogStream(): PrintStream;

        static println(arg0: String): void;
    }

    export class DriverPropertyInfo {
        name: String
        description: String
        required: boolean
        value: String
        choices: String[]
        constructor(arg0: String, arg1: String);
    }

    export class JDBCType extends Enum<JDBCType> implements SQLType {
        static BIT: JDBCType
        static TINYINT: JDBCType
        static SMALLINT: JDBCType
        static INTEGER: JDBCType
        static BIGINT: JDBCType
        static FLOAT: JDBCType
        static REAL: JDBCType
        static DOUBLE: JDBCType
        static NUMERIC: JDBCType
        static DECIMAL: JDBCType
        static CHAR: JDBCType
        static VARCHAR: JDBCType
        static LONGVARCHAR: JDBCType
        static DATE: JDBCType
        static TIME: JDBCType
        static TIMESTAMP: JDBCType
        static BINARY: JDBCType
        static VARBINARY: JDBCType
        static LONGVARBINARY: JDBCType
        static NULL: JDBCType
        static OTHER: JDBCType
        static JAVA_OBJECT: JDBCType
        static DISTINCT: JDBCType
        static STRUCT: JDBCType
        static ARRAY: JDBCType
        static BLOB: JDBCType
        static CLOB: JDBCType
        static REF: JDBCType
        static DATALINK: JDBCType
        static BOOLEAN: JDBCType
        static ROWID: JDBCType
        static NCHAR: JDBCType
        static NVARCHAR: JDBCType
        static LONGNVARCHAR: JDBCType
        static NCLOB: JDBCType
        static SQLXML: JDBCType
        static REF_CURSOR: JDBCType
        static TIME_WITH_TIMEZONE: JDBCType
        static TIMESTAMP_WITH_TIMEZONE: JDBCType

        static values(): JDBCType[];

        static valueOf(arg0: String): JDBCType;

        getName(): String;

        getVendor(): String;

        getVendorTypeNumber(): Number;

        static valueOf(arg0: number): JDBCType;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface NClob extends Clob {
    }

    export namespace ParameterMetaData {
        const parameterNoNulls: number
        const parameterNullable: number
        const parameterNullableUnknown: number
        const parameterModeUnknown: number
        const parameterModeIn: number
        const parameterModeInOut: number
        const parameterModeOut: number
    }

    export interface ParameterMetaData extends Wrapper {
        parameterNoNulls: number
        parameterNullable: number
        parameterNullableUnknown: number
        parameterModeUnknown: number
        parameterModeIn: number
        parameterModeInOut: number
        parameterModeOut: number

        getParameterCount(): number;

        isNullable(arg0: number): number;

        isSigned(arg0: number): boolean;

        getPrecision(arg0: number): number;

        getScale(arg0: number): number;

        getParameterType(arg0: number): number;

        getParameterTypeName(arg0: number): String;

        getParameterClassName(arg0: number): String;

        getParameterMode(arg0: number): number;
    }

    export interface PreparedStatement extends Statement {

        executeQuery(): ResultSet;

        executeUpdate(): number;

        setNull(arg0: number, arg1: number): void;

        setBoolean(arg0: number, arg1: boolean): void;

        setByte(arg0: number, arg1: number): void;

        setShort(arg0: number, arg1: number): void;

        setInt(arg0: number, arg1: number): void;

        setLong(arg0: number, arg1: number): void;

        setFloat(arg0: number, arg1: number): void;

        setDouble(arg0: number, arg1: number): void;

        setBigDecimal(arg0: number, arg1: BigDecimal): void;

        setString(arg0: number, arg1: String): void;

        setBytes(arg0: number, arg1: number[]): void;

        setDate(arg0: number, arg1: Date): void;

        setTime(arg0: number, arg1: Time): void;

        setTimestamp(arg0: number, arg1: Timestamp): void;

        setAsciiStream(arg0: number, arg1: InputStream, arg2: number): void;

        setUnicodeStream(arg0: number, arg1: InputStream, arg2: number): void;

        setBinaryStream(arg0: number, arg1: InputStream, arg2: number): void;

        clearParameters(): void;

        setObject(arg0: number, arg1: Object, arg2: number): void;

        setObject(arg0: number, arg1: Object): void;

        execute(): boolean;

        addBatch(): void;

        setCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        setRef(arg0: number, arg1: Ref): void;

        setBlob(arg0: number, arg1: Blob): void;

        setClob(arg0: number, arg1: Clob): void;

        setArray(arg0: number, arg1: Array): void;

        getMetaData(): ResultSetMetaData;

        setDate(arg0: number, arg1: Date, arg2: Calendar): void;

        setTime(arg0: number, arg1: Time, arg2: Calendar): void;

        setTimestamp(arg0: number, arg1: Timestamp, arg2: Calendar): void;

        setNull(arg0: number, arg1: number, arg2: String): void;

        setURL(arg0: number, arg1: URL): void;

        getParameterMetaData(): ParameterMetaData;

        setRowId(arg0: number, arg1: RowId): void;

        setNString(arg0: number, arg1: String): void;

        setNCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        setNClob(arg0: number, arg1: NClob): void;

        setClob(arg0: number, arg1: Reader, arg2: number): void;

        setBlob(arg0: number, arg1: InputStream, arg2: number): void;

        setNClob(arg0: number, arg1: Reader, arg2: number): void;

        setSQLXML(arg0: number, arg1: SQLXML): void;

        setObject(arg0: number, arg1: Object, arg2: number, arg3: number): void;

        setAsciiStream(arg0: number, arg1: InputStream, arg2: number): void;

        setBinaryStream(arg0: number, arg1: InputStream, arg2: number): void;

        setCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        setAsciiStream(arg0: number, arg1: InputStream): void;

        setBinaryStream(arg0: number, arg1: InputStream): void;

        setCharacterStream(arg0: number, arg1: Reader): void;

        setNCharacterStream(arg0: number, arg1: Reader): void;

        setClob(arg0: number, arg1: Reader): void;

        setBlob(arg0: number, arg1: InputStream): void;

        setNClob(arg0: number, arg1: Reader): void;

/* default */ setObject(arg0: number, arg1: Object, arg2: SQLType, arg3: number): void;

/* default */ setObject(arg0: number, arg1: Object, arg2: SQLType): void;

/* default */ executeLargeUpdate(): number;
    }

    export class PseudoColumnUsage extends Enum<PseudoColumnUsage> {
        static SELECT_LIST_ONLY: PseudoColumnUsage
        static WHERE_CLAUSE_ONLY: PseudoColumnUsage
        static NO_USAGE_RESTRICTIONS: PseudoColumnUsage
        static USAGE_UNKNOWN: PseudoColumnUsage

        static values(): PseudoColumnUsage[];

        static valueOf(arg0: String): PseudoColumnUsage;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface Ref {

        getBaseTypeName(): String;

        getObject(arg0: Map<String, Class<any>>): Object;

        getObject(): Object;

        setObject(arg0: Object): void;
    }

    export namespace ResultSet {
        const FETCH_FORWARD: number
        const FETCH_REVERSE: number
        const FETCH_UNKNOWN: number
        const TYPE_FORWARD_ONLY: number
        const TYPE_SCROLL_INSENSITIVE: number
        const TYPE_SCROLL_SENSITIVE: number
        const CONCUR_READ_ONLY: number
        const CONCUR_UPDATABLE: number
        const HOLD_CURSORS_OVER_COMMIT: number
        const CLOSE_CURSORS_AT_COMMIT: number
    }

    export interface ResultSet extends Wrapper, AutoCloseable {
        FETCH_FORWARD: number
        FETCH_REVERSE: number
        FETCH_UNKNOWN: number
        TYPE_FORWARD_ONLY: number
        TYPE_SCROLL_INSENSITIVE: number
        TYPE_SCROLL_SENSITIVE: number
        CONCUR_READ_ONLY: number
        CONCUR_UPDATABLE: number
        HOLD_CURSORS_OVER_COMMIT: number
        CLOSE_CURSORS_AT_COMMIT: number

        next(): boolean;

        close(): void;

        wasNull(): boolean;

        getString(arg0: number): String;

        getBoolean(arg0: number): boolean;

        getByte(arg0: number): number;

        getShort(arg0: number): number;

        getInt(arg0: number): number;

        getLong(arg0: number): number;

        getFloat(arg0: number): number;

        getDouble(arg0: number): number;

        getBigDecimal(arg0: number, arg1: number): BigDecimal;

        getBytes(arg0: number): number[];

        getDate(arg0: number): Date;

        getTime(arg0: number): Time;

        getTimestamp(arg0: number): Timestamp;

        getAsciiStream(arg0: number): InputStream;

        getUnicodeStream(arg0: number): InputStream;

        getBinaryStream(arg0: number): InputStream;

        getString(arg0: String): String;

        getBoolean(arg0: String): boolean;

        getByte(arg0: String): number;

        getShort(arg0: String): number;

        getInt(arg0: String): number;

        getLong(arg0: String): number;

        getFloat(arg0: String): number;

        getDouble(arg0: String): number;

        getBigDecimal(arg0: String, arg1: number): BigDecimal;

        getBytes(arg0: String): number[];

        getDate(arg0: String): Date;

        getTime(arg0: String): Time;

        getTimestamp(arg0: String): Timestamp;

        getAsciiStream(arg0: String): InputStream;

        getUnicodeStream(arg0: String): InputStream;

        getBinaryStream(arg0: String): InputStream;

        getWarnings(): SQLWarning;

        clearWarnings(): void;

        getCursorName(): String;

        getMetaData(): ResultSetMetaData;

        getObject(arg0: number): Object;

        getObject(arg0: String): Object;

        findColumn(arg0: String): number;

        getCharacterStream(arg0: number): Reader;

        getCharacterStream(arg0: String): Reader;

        getBigDecimal(arg0: number): BigDecimal;

        getBigDecimal(arg0: String): BigDecimal;

        isBeforeFirst(): boolean;

        isAfterLast(): boolean;

        isFirst(): boolean;

        isLast(): boolean;

        beforeFirst(): void;

        afterLast(): void;

        first(): boolean;

        last(): boolean;

        getRow(): number;

        absolute(arg0: number): boolean;

        relative(arg0: number): boolean;

        previous(): boolean;

        setFetchDirection(arg0: number): void;

        getFetchDirection(): number;

        setFetchSize(arg0: number): void;

        getFetchSize(): number;

        getType(): number;

        getConcurrency(): number;

        rowUpdated(): boolean;

        rowInserted(): boolean;

        rowDeleted(): boolean;

        updateNull(arg0: number): void;

        updateBoolean(arg0: number, arg1: boolean): void;

        updateByte(arg0: number, arg1: number): void;

        updateShort(arg0: number, arg1: number): void;

        updateInt(arg0: number, arg1: number): void;

        updateLong(arg0: number, arg1: number): void;

        updateFloat(arg0: number, arg1: number): void;

        updateDouble(arg0: number, arg1: number): void;

        updateBigDecimal(arg0: number, arg1: BigDecimal): void;

        updateString(arg0: number, arg1: String): void;

        updateBytes(arg0: number, arg1: number[]): void;

        updateDate(arg0: number, arg1: Date): void;

        updateTime(arg0: number, arg1: Time): void;

        updateTimestamp(arg0: number, arg1: Timestamp): void;

        updateAsciiStream(arg0: number, arg1: InputStream, arg2: number): void;

        updateBinaryStream(arg0: number, arg1: InputStream, arg2: number): void;

        updateCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        updateObject(arg0: number, arg1: Object, arg2: number): void;

        updateObject(arg0: number, arg1: Object): void;

        updateNull(arg0: String): void;

        updateBoolean(arg0: String, arg1: boolean): void;

        updateByte(arg0: String, arg1: number): void;

        updateShort(arg0: String, arg1: number): void;

        updateInt(arg0: String, arg1: number): void;

        updateLong(arg0: String, arg1: number): void;

        updateFloat(arg0: String, arg1: number): void;

        updateDouble(arg0: String, arg1: number): void;

        updateBigDecimal(arg0: String, arg1: BigDecimal): void;

        updateString(arg0: String, arg1: String): void;

        updateBytes(arg0: String, arg1: number[]): void;

        updateDate(arg0: String, arg1: Date): void;

        updateTime(arg0: String, arg1: Time): void;

        updateTimestamp(arg0: String, arg1: Timestamp): void;

        updateAsciiStream(arg0: String, arg1: InputStream, arg2: number): void;

        updateBinaryStream(arg0: String, arg1: InputStream, arg2: number): void;

        updateCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        updateObject(arg0: String, arg1: Object, arg2: number): void;

        updateObject(arg0: String, arg1: Object): void;

        insertRow(): void;

        updateRow(): void;

        deleteRow(): void;

        refreshRow(): void;

        cancelRowUpdates(): void;

        moveToInsertRow(): void;

        moveToCurrentRow(): void;

        getStatement(): Statement;

        getObject(arg0: number, arg1: Map<String, Class<any>>): Object;

        getRef(arg0: number): Ref;

        getBlob(arg0: number): Blob;

        getClob(arg0: number): Clob;

        getArray(arg0: number): Array;

        getObject(arg0: String, arg1: Map<String, Class<any>>): Object;

        getRef(arg0: String): Ref;

        getBlob(arg0: String): Blob;

        getClob(arg0: String): Clob;

        getArray(arg0: String): Array;

        getDate(arg0: number, arg1: Calendar): Date;

        getDate(arg0: String, arg1: Calendar): Date;

        getTime(arg0: number, arg1: Calendar): Time;

        getTime(arg0: String, arg1: Calendar): Time;

        getTimestamp(arg0: number, arg1: Calendar): Timestamp;

        getTimestamp(arg0: String, arg1: Calendar): Timestamp;

        getURL(arg0: number): URL;

        getURL(arg0: String): URL;

        updateRef(arg0: number, arg1: Ref): void;

        updateRef(arg0: String, arg1: Ref): void;

        updateBlob(arg0: number, arg1: Blob): void;

        updateBlob(arg0: String, arg1: Blob): void;

        updateClob(arg0: number, arg1: Clob): void;

        updateClob(arg0: String, arg1: Clob): void;

        updateArray(arg0: number, arg1: Array): void;

        updateArray(arg0: String, arg1: Array): void;

        getRowId(arg0: number): RowId;

        getRowId(arg0: String): RowId;

        updateRowId(arg0: number, arg1: RowId): void;

        updateRowId(arg0: String, arg1: RowId): void;

        getHoldability(): number;

        isClosed(): boolean;

        updateNString(arg0: number, arg1: String): void;

        updateNString(arg0: String, arg1: String): void;

        updateNClob(arg0: number, arg1: NClob): void;

        updateNClob(arg0: String, arg1: NClob): void;

        getNClob(arg0: number): NClob;

        getNClob(arg0: String): NClob;

        getSQLXML(arg0: number): SQLXML;

        getSQLXML(arg0: String): SQLXML;

        updateSQLXML(arg0: number, arg1: SQLXML): void;

        updateSQLXML(arg0: String, arg1: SQLXML): void;

        getNString(arg0: number): String;

        getNString(arg0: String): String;

        getNCharacterStream(arg0: number): Reader;

        getNCharacterStream(arg0: String): Reader;

        updateNCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        updateNCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        updateAsciiStream(arg0: number, arg1: InputStream, arg2: number): void;

        updateBinaryStream(arg0: number, arg1: InputStream, arg2: number): void;

        updateCharacterStream(arg0: number, arg1: Reader, arg2: number): void;

        updateAsciiStream(arg0: String, arg1: InputStream, arg2: number): void;

        updateBinaryStream(arg0: String, arg1: InputStream, arg2: number): void;

        updateCharacterStream(arg0: String, arg1: Reader, arg2: number): void;

        updateBlob(arg0: number, arg1: InputStream, arg2: number): void;

        updateBlob(arg0: String, arg1: InputStream, arg2: number): void;

        updateClob(arg0: number, arg1: Reader, arg2: number): void;

        updateClob(arg0: String, arg1: Reader, arg2: number): void;

        updateNClob(arg0: number, arg1: Reader, arg2: number): void;

        updateNClob(arg0: String, arg1: Reader, arg2: number): void;

        updateNCharacterStream(arg0: number, arg1: Reader): void;

        updateNCharacterStream(arg0: String, arg1: Reader): void;

        updateAsciiStream(arg0: number, arg1: InputStream): void;

        updateBinaryStream(arg0: number, arg1: InputStream): void;

        updateCharacterStream(arg0: number, arg1: Reader): void;

        updateAsciiStream(arg0: String, arg1: InputStream): void;

        updateBinaryStream(arg0: String, arg1: InputStream): void;

        updateCharacterStream(arg0: String, arg1: Reader): void;

        updateBlob(arg0: number, arg1: InputStream): void;

        updateBlob(arg0: String, arg1: InputStream): void;

        updateClob(arg0: number, arg1: Reader): void;

        updateClob(arg0: String, arg1: Reader): void;

        updateNClob(arg0: number, arg1: Reader): void;

        updateNClob(arg0: String, arg1: Reader): void;

        getObject<T extends Object>(arg0: number, arg1: Class<T>): T;

        getObject<T extends Object>(arg0: String, arg1: Class<T>): T;

/* default */ updateObject(arg0: number, arg1: Object, arg2: SQLType, arg3: number): void;

/* default */ updateObject(arg0: String, arg1: Object, arg2: SQLType, arg3: number): void;

/* default */ updateObject(arg0: number, arg1: Object, arg2: SQLType): void;

/* default */ updateObject(arg0: String, arg1: Object, arg2: SQLType): void;
    }

    export namespace ResultSetMetaData {
        const columnNoNulls: number
        const columnNullable: number
        const columnNullableUnknown: number
    }

    export interface ResultSetMetaData extends Wrapper {
        columnNoNulls: number
        columnNullable: number
        columnNullableUnknown: number

        getColumnCount(): number;

        isAutoIncrement(arg0: number): boolean;

        isCaseSensitive(arg0: number): boolean;

        isSearchable(arg0: number): boolean;

        isCurrency(arg0: number): boolean;

        isNullable(arg0: number): number;

        isSigned(arg0: number): boolean;

        getColumnDisplaySize(arg0: number): number;

        getColumnLabel(arg0: number): String;

        getColumnName(arg0: number): String;

        getSchemaName(arg0: number): String;

        getPrecision(arg0: number): number;

        getScale(arg0: number): number;

        getTableName(arg0: number): String;

        getCatalogName(arg0: number): String;

        getColumnType(arg0: number): number;

        getColumnTypeName(arg0: number): String;

        isReadOnly(arg0: number): boolean;

        isWritable(arg0: number): boolean;

        isDefinitelyWritable(arg0: number): boolean;

        getColumnClassName(arg0: number): String;
    }

    export interface RowId {

        equals(arg0: Object): boolean;

        getBytes(): number[];
        toString(): string;

        hashCode(): number;
    }

    export class RowIdLifetime extends Enum<RowIdLifetime> {
        static ROWID_UNSUPPORTED: RowIdLifetime
        static ROWID_VALID_OTHER: RowIdLifetime
        static ROWID_VALID_SESSION: RowIdLifetime
        static ROWID_VALID_TRANSACTION: RowIdLifetime
        static ROWID_VALID_FOREVER: RowIdLifetime

        static values(): RowIdLifetime[];

        static valueOf(arg0: String): RowIdLifetime;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface SQLClientInfoException { }
    export class SQLClientInfoException extends SQLException {
        constructor();
        constructor(arg0: Map<String, ClientInfoStatus>);
        constructor(arg0: Map<String, ClientInfoStatus>, arg1: Throwable);
        constructor(arg0: String, arg1: Map<String, ClientInfoStatus>);
        constructor(arg0: String, arg1: Map<String, ClientInfoStatus>, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: Map<String, ClientInfoStatus>);
        constructor(arg0: String, arg1: String, arg2: Map<String, ClientInfoStatus>, arg3: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Map<String, ClientInfoStatus>);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Map<String, ClientInfoStatus>, arg4: Throwable);

        getFailedProperties(): Map<String, ClientInfoStatus>;
    }

    export interface SQLData {

        getSQLTypeName(): String;

        readSQL(arg0: SQLInput, arg1: String): void;

        writeSQL(arg0: SQLOutput): void;
    }

    export interface SQLDataException { }
    export class SQLDataException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLException extends Iterable<Throwable> { }
    export class SQLException extends Exception implements Iterable<Throwable> {
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String);
        constructor();
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);

        getSQLState(): String;

        getErrorCode(): number;

        getNextException(): SQLException;

        setNextException(arg0: SQLException): void;

        iterator(): Iterator<Throwable>;
    }

    export interface SQLFeatureNotSupportedException { }
    export class SQLFeatureNotSupportedException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLInput {

        readString(): String;

        readBoolean(): boolean;

        readByte(): number;

        readShort(): number;

        readInt(): number;

        readLong(): number;

        readFloat(): number;

        readDouble(): number;

        readBigDecimal(): BigDecimal;

        readBytes(): number[];

        readDate(): Date;

        readTime(): Time;

        readTimestamp(): Timestamp;

        readCharacterStream(): Reader;

        readAsciiStream(): InputStream;

        readBinaryStream(): InputStream;

        readObject(): Object;

        readRef(): Ref;

        readBlob(): Blob;

        readClob(): Clob;

        readArray(): Array;

        wasNull(): boolean;

        readURL(): URL;

        readNClob(): NClob;

        readNString(): String;

        readSQLXML(): SQLXML;

        readRowId(): RowId;

/* default */ readObject<T extends Object>(arg0: Class<T>): T;
    }

    export interface SQLIntegrityConstraintViolationException { }
    export class SQLIntegrityConstraintViolationException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLInvalidAuthorizationSpecException { }
    export class SQLInvalidAuthorizationSpecException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLNonTransientConnectionException { }
    export class SQLNonTransientConnectionException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLNonTransientException { }
    export class SQLNonTransientException extends SQLException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLOutput {

        writeString(arg0: String): void;

        writeBoolean(arg0: boolean): void;

        writeByte(arg0: number): void;

        writeShort(arg0: number): void;

        writeInt(arg0: number): void;

        writeLong(arg0: number): void;

        writeFloat(arg0: number): void;

        writeDouble(arg0: number): void;

        writeBigDecimal(arg0: BigDecimal): void;

        writeBytes(arg0: number[]): void;

        writeDate(arg0: Date): void;

        writeTime(arg0: Time): void;

        writeTimestamp(arg0: Timestamp): void;

        writeCharacterStream(arg0: Reader): void;

        writeAsciiStream(arg0: InputStream): void;

        writeBinaryStream(arg0: InputStream): void;

        writeObject(arg0: SQLData): void;

        writeRef(arg0: Ref): void;

        writeBlob(arg0: Blob): void;

        writeClob(arg0: Clob): void;

        writeStruct(arg0: Struct): void;

        writeArray(arg0: Array): void;

        writeURL(arg0: URL): void;

        writeNString(arg0: String): void;

        writeNClob(arg0: NClob): void;

        writeRowId(arg0: RowId): void;

        writeSQLXML(arg0: SQLXML): void;

/* default */ writeObject(arg0: Object, arg1: SQLType): void;
    }

    export class SQLPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export interface SQLRecoverableException { }
    export class SQLRecoverableException extends SQLException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLSyntaxErrorException { }
    export class SQLSyntaxErrorException extends SQLNonTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLTimeoutException { }
    export class SQLTimeoutException extends SQLTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLTransactionRollbackException { }
    export class SQLTransactionRollbackException extends SQLTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLTransientConnectionException { }
    export class SQLTransientConnectionException extends SQLTransientException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLTransientException { }
    export class SQLTransientException extends SQLException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);
    }

    export interface SQLType {

        getName(): String;

        getVendor(): String;

        getVendorTypeNumber(): Number;
    }

    export interface SQLWarning { }
    export class SQLWarning extends SQLException {
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String);
        constructor();
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: String, arg2: Throwable);
        constructor(arg0: String, arg1: String, arg2: number, arg3: Throwable);

        getNextWarning(): SQLWarning;

        setNextWarning(arg0: SQLWarning): void;
    }

    export interface SQLXML {

        free(): void;

        getBinaryStream(): InputStream;

        setBinaryStream(): OutputStream;

        getCharacterStream(): Reader;

        setCharacterStream(): Writer;

        getString(): String;

        setString(arg0: String): void;

        getSource<T extends Source>(arg0: Class<T>): T;

        setResult<T extends Result>(arg0: Class<T>): T;
    }

    export interface Savepoint {

        getSavepointId(): number;

        getSavepointName(): String;
    }

    export interface ShardingKey {
    }

    export interface ShardingKeyBuilder {

        subkey(arg0: Object, arg1: SQLType): ShardingKeyBuilder;

        build(): ShardingKey;
    }

    export namespace Statement {
        const CLOSE_CURRENT_RESULT: number
        const KEEP_CURRENT_RESULT: number
        const CLOSE_ALL_RESULTS: number
        const SUCCESS_NO_INFO: number
        const EXECUTE_FAILED: number
        const RETURN_GENERATED_KEYS: number
        const NO_GENERATED_KEYS: number
    }

    export interface Statement extends Wrapper, AutoCloseable {
        CLOSE_CURRENT_RESULT: number
        KEEP_CURRENT_RESULT: number
        CLOSE_ALL_RESULTS: number
        SUCCESS_NO_INFO: number
        EXECUTE_FAILED: number
        RETURN_GENERATED_KEYS: number
        NO_GENERATED_KEYS: number

        executeQuery(arg0: String): ResultSet;

        executeUpdate(arg0: String): number;

        close(): void;

        getMaxFieldSize(): number;

        setMaxFieldSize(arg0: number): void;

        getMaxRows(): number;

        setMaxRows(arg0: number): void;

        setEscapeProcessing(arg0: boolean): void;

        getQueryTimeout(): number;

        setQueryTimeout(arg0: number): void;

        cancel(): void;

        getWarnings(): SQLWarning;

        clearWarnings(): void;

        setCursorName(arg0: String): void;

        execute(arg0: String): boolean;

        getResultSet(): ResultSet;

        getUpdateCount(): number;

        getMoreResults(): boolean;

        setFetchDirection(arg0: number): void;

        getFetchDirection(): number;

        setFetchSize(arg0: number): void;

        getFetchSize(): number;

        getResultSetConcurrency(): number;

        getResultSetType(): number;

        addBatch(arg0: String): void;

        clearBatch(): void;

        executeBatch(): number[];

        getConnection(): Connection;

        getMoreResults(arg0: number): boolean;

        getGeneratedKeys(): ResultSet;

        executeUpdate(arg0: String, arg1: number): number;

        executeUpdate(arg0: String, arg1: number[]): number;

        executeUpdate(arg0: String, arg1: String[]): number;

        execute(arg0: String, arg1: number): boolean;

        execute(arg0: String, arg1: number[]): boolean;

        execute(arg0: String, arg1: String[]): boolean;

        getResultSetHoldability(): number;

        isClosed(): boolean;

        setPoolable(arg0: boolean): void;

        isPoolable(): boolean;

        closeOnCompletion(): void;

        isCloseOnCompletion(): boolean;

/* default */ getLargeUpdateCount(): number;

/* default */ setLargeMaxRows(arg0: number): void;

/* default */ getLargeMaxRows(): number;

/* default */ executeLargeBatch(): number[];

/* default */ executeLargeUpdate(arg0: String): number;

/* default */ executeLargeUpdate(arg0: String, arg1: number): number;

/* default */ executeLargeUpdate(arg0: String, arg1: number[]): number;

/* default */ executeLargeUpdate(arg0: String, arg1: String[]): number;

/* default */ enquoteLiteral(arg0: String): String;

/* default */ enquoteIdentifier(arg0: String, arg1: boolean): String;

/* default */ isSimpleIdentifier(arg0: String): boolean;

/* default */ enquoteNCharLiteral(arg0: String): String;
    }

    export interface Struct {

        getSQLTypeName(): String;

        getAttributes(): Object[];

        getAttributes(arg0: Map<String, Class<any>>): Object[];
    }

    export class Time extends Date {
        constructor(arg0: number, arg1: number, arg2: number);
        constructor(arg0: number);

        setTime(arg0: number): void;

        static valueOf(arg0: String): Time;
        toString(): string;

        getYear(): number;

        getMonth(): number;

        getDay(): number;

        getDate(): number;

        setYear(arg0: number): void;

        setMonth(arg0: number): void;

        setDate(arg0: number): void;

        static valueOf(arg0: LocalTime): Time;

        toLocalTime(): LocalTime;

        toInstant(): Instant;
    }

    export class Timestamp extends Date {
        constructor(arg0: number, arg1: number, arg2: number, arg3: number, arg4: number, arg5: number, arg6: number);
        constructor(arg0: number);

        setTime(arg0: number): void;

        getTime(): number;

        static valueOf(arg0: String): Timestamp;
        toString(): string;

        getNanos(): number;

        setNanos(arg0: number): void;

        equals(arg0: Timestamp): boolean;

        equals(arg0: Object): boolean;

        before(arg0: Timestamp): boolean;

        after(arg0: Timestamp): boolean;

        compareTo(arg0: Timestamp): number;

        compareTo(arg0: Date): number;

        hashCode(): number;

        static valueOf(arg0: LocalDateTime): Timestamp;

        toLocalDateTime(): LocalDateTime;

        static from(arg0: Instant): Timestamp;

        toInstant(): Instant;
    }

    export class Types {
        static BIT: number
        static TINYINT: number
        static SMALLINT: number
        static INTEGER: number
        static BIGINT: number
        static FLOAT: number
        static REAL: number
        static DOUBLE: number
        static NUMERIC: number
        static DECIMAL: number
        static CHAR: number
        static VARCHAR: number
        static LONGVARCHAR: number
        static DATE: number
        static TIME: number
        static TIMESTAMP: number
        static BINARY: number
        static VARBINARY: number
        static LONGVARBINARY: number
        static NULL: number
        static OTHER: number
        static JAVA_OBJECT: number
        static DISTINCT: number
        static STRUCT: number
        static ARRAY: number
        static BLOB: number
        static CLOB: number
        static REF: number
        static DATALINK: number
        static BOOLEAN: number
        static ROWID: number
        static NCHAR: number
        static NVARCHAR: number
        static LONGNVARCHAR: number
        static NCLOB: number
        static SQLXML: number
        static REF_CURSOR: number
        static TIME_WITH_TIMEZONE: number
        static TIMESTAMP_WITH_TIMEZONE: number
    }

    export interface Wrapper {

        unwrap<T extends Object>(arg0: Class<T>): T;

        isWrapperFor(arg0: Class<any>): boolean;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.security.interfaces.d.ts" />
/// <reference path="java.math.d.ts" />
declare module '@java/java.security.spec' {
    import { GeneralSecurityException } from '@java/java.security'
    import { Throwable, String } from '@java/java.lang'
    import { Optional } from '@java/java.util'
    import { DSAParams } from '@java/java.security.interfaces'
    import { BigInteger } from '@java/java.math'
    export interface AlgorithmParameterSpec {
    }

    export class DSAGenParameterSpec implements AlgorithmParameterSpec {
        constructor(arg0: number, arg1: number);
        constructor(arg0: number, arg1: number, arg2: number);

        getPrimePLength(): number;

        getSubprimeQLength(): number;

        getSeedLength(): number;
    }

    export class DSAParameterSpec implements AlgorithmParameterSpec, DSAParams {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger);

        getP(): BigInteger;

        getQ(): BigInteger;

        getG(): BigInteger;
    }

    export class DSAPrivateKeySpec implements KeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger);

        getX(): BigInteger;

        getP(): BigInteger;

        getQ(): BigInteger;

        getG(): BigInteger;
    }

    export class DSAPublicKeySpec implements KeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger);

        getY(): BigInteger;

        getP(): BigInteger;

        getQ(): BigInteger;

        getG(): BigInteger;
    }

    export interface ECField {

        getFieldSize(): number;
    }

    export class ECFieldF2m implements ECField {
        constructor(arg0: number);
        constructor(arg0: number, arg1: BigInteger);
        constructor(arg0: number, arg1: number[]);

        getFieldSize(): number;

        getM(): number;

        getReductionPolynomial(): BigInteger;

        getMidTermsOfReductionPolynomial(): number[];

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class ECFieldFp implements ECField {
        constructor(arg0: BigInteger);

        getFieldSize(): number;

        getP(): BigInteger;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class ECGenParameterSpec extends NamedParameterSpec {
        constructor(arg0: String);
    }

    export class ECParameterSpec implements AlgorithmParameterSpec {
        constructor(arg0: EllipticCurve, arg1: ECPoint, arg2: BigInteger, arg3: number);

        getCurve(): EllipticCurve;

        getGenerator(): ECPoint;

        getOrder(): BigInteger;

        getCofactor(): number;
    }

    export class ECPoint {
        static POINT_INFINITY: ECPoint
        constructor(arg0: BigInteger, arg1: BigInteger);

        getAffineX(): BigInteger;

        getAffineY(): BigInteger;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class ECPrivateKeySpec implements KeySpec {
        constructor(arg0: BigInteger, arg1: ECParameterSpec);

        getS(): BigInteger;

        getParams(): ECParameterSpec;
    }

    export class ECPublicKeySpec implements KeySpec {
        constructor(arg0: ECPoint, arg1: ECParameterSpec);

        getW(): ECPoint;

        getParams(): ECParameterSpec;
    }

    export class EdDSAParameterSpec implements AlgorithmParameterSpec {
        constructor(arg0: boolean);
        constructor(arg0: boolean, arg1: number[]);

        isPrehash(): boolean;

        getContext(): Optional<number[]>;
    }

    export class EdECPoint {
        constructor(arg0: boolean, arg1: BigInteger);

        isXOdd(): boolean;

        getY(): BigInteger;
    }

    export class EdECPrivateKeySpec implements KeySpec {
        constructor(arg0: NamedParameterSpec, arg1: number[]);

        getParams(): NamedParameterSpec;

        getBytes(): number[];
    }

    export class EdECPublicKeySpec implements KeySpec {
        constructor(arg0: NamedParameterSpec, arg1: EdECPoint);

        getParams(): NamedParameterSpec;

        getPoint(): EdECPoint;
    }

    export class EllipticCurve {
        constructor(arg0: ECField, arg1: BigInteger, arg2: BigInteger);
        constructor(arg0: ECField, arg1: BigInteger, arg2: BigInteger, arg3: number[]);

        getField(): ECField;

        getA(): BigInteger;

        getB(): BigInteger;

        getSeed(): number[];

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export abstract class EncodedKeySpec implements KeySpec {
        constructor(arg0: number[]);

        getAlgorithm(): String;

        getEncoded(): number[];

        abstract getFormat(): String;
    }

    export class InvalidKeySpecException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class InvalidParameterSpecException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
    }

    export interface KeySpec {
    }

    export class MGF1ParameterSpec implements AlgorithmParameterSpec {
        static SHA1: MGF1ParameterSpec
        static SHA224: MGF1ParameterSpec
        static SHA256: MGF1ParameterSpec
        static SHA384: MGF1ParameterSpec
        static SHA512: MGF1ParameterSpec
        static SHA512_224: MGF1ParameterSpec
        static SHA512_256: MGF1ParameterSpec
        static SHA3_224: MGF1ParameterSpec
        static SHA3_256: MGF1ParameterSpec
        static SHA3_384: MGF1ParameterSpec
        static SHA3_512: MGF1ParameterSpec
        constructor(arg0: String);

        getDigestAlgorithm(): String;
        toString(): string;
    }

    export class NamedParameterSpec implements AlgorithmParameterSpec {
        static X25519: NamedParameterSpec
        static X448: NamedParameterSpec
        static ED25519: NamedParameterSpec
        static ED448: NamedParameterSpec
        constructor(arg0: String);

        getName(): String;
    }

    export class PKCS8EncodedKeySpec extends EncodedKeySpec {
        constructor(arg0: number[]);
        constructor(arg0: number[], arg1: String);

        getEncoded(): number[];

        getFormat(): String;
    }

    export class PSSParameterSpec implements AlgorithmParameterSpec {
        static TRAILER_FIELD_BC: number
        static DEFAULT: PSSParameterSpec
        constructor(arg0: String, arg1: String, arg2: AlgorithmParameterSpec, arg3: number, arg4: number);
        constructor(arg0: number);

        getDigestAlgorithm(): String;

        getMGFAlgorithm(): String;

        getMGFParameters(): AlgorithmParameterSpec;

        getSaltLength(): number;

        getTrailerField(): number;
        toString(): string;
    }

    export class RSAKeyGenParameterSpec implements AlgorithmParameterSpec {
        static F0: BigInteger
        static F4: BigInteger
        constructor(arg0: number, arg1: BigInteger);
        constructor(arg0: number, arg1: BigInteger, arg2: AlgorithmParameterSpec);

        getKeysize(): number;

        getPublicExponent(): BigInteger;

        getKeyParams(): AlgorithmParameterSpec;
    }

    export class RSAMultiPrimePrivateCrtKeySpec extends RSAPrivateKeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger, arg4: BigInteger, arg5: BigInteger, arg6: BigInteger, arg7: BigInteger, arg8: RSAOtherPrimeInfo[]);
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger, arg4: BigInteger, arg5: BigInteger, arg6: BigInteger, arg7: BigInteger, arg8: RSAOtherPrimeInfo[], arg9: AlgorithmParameterSpec);

        getPublicExponent(): BigInteger;

        getPrimeP(): BigInteger;

        getPrimeQ(): BigInteger;

        getPrimeExponentP(): BigInteger;

        getPrimeExponentQ(): BigInteger;

        getCrtCoefficient(): BigInteger;

        getOtherPrimeInfo(): RSAOtherPrimeInfo[];
    }

    export class RSAOtherPrimeInfo {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger);

        getPrime(): BigInteger;

        getExponent(): BigInteger;

        getCrtCoefficient(): BigInteger;
    }

    export class RSAPrivateCrtKeySpec extends RSAPrivateKeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger, arg4: BigInteger, arg5: BigInteger, arg6: BigInteger, arg7: BigInteger);
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: BigInteger, arg3: BigInteger, arg4: BigInteger, arg5: BigInteger, arg6: BigInteger, arg7: BigInteger, arg8: AlgorithmParameterSpec);

        getPublicExponent(): BigInteger;

        getPrimeP(): BigInteger;

        getPrimeQ(): BigInteger;

        getPrimeExponentP(): BigInteger;

        getPrimeExponentQ(): BigInteger;

        getCrtCoefficient(): BigInteger;
    }

    export class RSAPrivateKeySpec implements KeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger);
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: AlgorithmParameterSpec);

        getModulus(): BigInteger;

        getPrivateExponent(): BigInteger;

        getParams(): AlgorithmParameterSpec;
    }

    export class RSAPublicKeySpec implements KeySpec {
        constructor(arg0: BigInteger, arg1: BigInteger);
        constructor(arg0: BigInteger, arg1: BigInteger, arg2: AlgorithmParameterSpec);

        getModulus(): BigInteger;

        getPublicExponent(): BigInteger;

        getParams(): AlgorithmParameterSpec;
    }

    export class X509EncodedKeySpec extends EncodedKeySpec {
        constructor(arg0: number[]);
        constructor(arg0: number[], arg1: String);

        getEncoded(): number[];

        getFormat(): String;
    }

    export class XECPrivateKeySpec implements KeySpec {
        constructor(arg0: AlgorithmParameterSpec, arg1: number[]);

        getParams(): AlgorithmParameterSpec;

        getScalar(): number[];
    }

    export class XECPublicKeySpec implements KeySpec {
        constructor(arg0: AlgorithmParameterSpec, arg1: BigInteger);

        getParams(): AlgorithmParameterSpec;

        getU(): BigInteger;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.math.d.ts" />
/// <reference path="java.security.spec.d.ts" />
declare module '@java/java.security.interfaces' {
    import { SecureRandom, PublicKey, PrivateKey } from '@java/java.security'
    import { Optional } from '@java/java.util'
    import { BigInteger } from '@java/java.math'
    import { RSAOtherPrimeInfo, NamedParameterSpec, ECPoint, AlgorithmParameterSpec, ECParameterSpec, EdECPoint } from '@java/java.security.spec'
    export interface DSAKey {

        getParams(): DSAParams;
    }

    export interface DSAKeyPairGenerator {

        initialize(arg0: DSAParams, arg1: SecureRandom): void;

        initialize(arg0: number, arg1: boolean, arg2: SecureRandom): void;
    }

    export interface DSAParams {

        getP(): BigInteger;

        getQ(): BigInteger;

        getG(): BigInteger;
    }

    export namespace DSAPrivateKey {
        const serialVersionUID: number
    }

    export interface DSAPrivateKey extends DSAKey, PrivateKey {
        serialVersionUID: number

        getX(): BigInteger;
    }

    export namespace DSAPublicKey {
        const serialVersionUID: number
    }

    export interface DSAPublicKey extends DSAKey, PublicKey {
        serialVersionUID: number

        getY(): BigInteger;
    }

    export interface ECKey {

        getParams(): ECParameterSpec;
    }

    export namespace ECPrivateKey {
        const serialVersionUID: number
    }

    export interface ECPrivateKey extends PrivateKey, ECKey {
        serialVersionUID: number

        getS(): BigInteger;
    }

    export namespace ECPublicKey {
        const serialVersionUID: number
    }

    export interface ECPublicKey extends PublicKey, ECKey {
        serialVersionUID: number

        getW(): ECPoint;
    }

    export interface EdECKey {

        getParams(): NamedParameterSpec;
    }

    export interface EdECPrivateKey extends EdECKey, PrivateKey {

        getBytes(): Optional<number[]>;
    }

    export interface EdECPublicKey extends EdECKey, PublicKey {

        getPoint(): EdECPoint;
    }

    export interface RSAKey {

        getModulus(): BigInteger;

/* default */ getParams(): AlgorithmParameterSpec;
    }

    export namespace RSAMultiPrimePrivateCrtKey {
        const serialVersionUID: number
    }

    export interface RSAMultiPrimePrivateCrtKey extends RSAPrivateKey {
        serialVersionUID: number

        getPublicExponent(): BigInteger;

        getPrimeP(): BigInteger;

        getPrimeQ(): BigInteger;

        getPrimeExponentP(): BigInteger;

        getPrimeExponentQ(): BigInteger;

        getCrtCoefficient(): BigInteger;

        getOtherPrimeInfo(): RSAOtherPrimeInfo[];
    }

    export namespace RSAPrivateCrtKey {
        const serialVersionUID: number
    }

    export interface RSAPrivateCrtKey extends RSAPrivateKey {
        serialVersionUID: number

        getPublicExponent(): BigInteger;

        getPrimeP(): BigInteger;

        getPrimeQ(): BigInteger;

        getPrimeExponentP(): BigInteger;

        getPrimeExponentQ(): BigInteger;

        getCrtCoefficient(): BigInteger;
    }

    export namespace RSAPrivateKey {
        const serialVersionUID: number
    }

    export interface RSAPrivateKey extends PrivateKey, RSAKey {
        serialVersionUID: number

        getPrivateExponent(): BigInteger;
    }

    export namespace RSAPublicKey {
        const serialVersionUID: number
    }

    export interface RSAPublicKey extends PublicKey, RSAKey {
        serialVersionUID: number

        getPublicExponent(): BigInteger;
    }

    export interface XECKey {

        getParams(): AlgorithmParameterSpec;
    }

    export interface XECPrivateKey extends XECKey, PrivateKey {

        getScalar(): Optional<number[]>;
    }

    export interface XECPublicKey extends XECKey, PublicKey {

        getU(): BigInteger;
    }

}
/// <reference path="javax.crypto.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.security.cert.d.ts" />
/// <reference path="java.security.spec.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="javax.security.auth.callback.d.ts" />
/// <reference path="javax.security.auth.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="javax.security.auth.login.d.ts" />
declare module '@java/java.security' {
    import { SecretKey } from '@java/javax.crypto'
    import { Set, Enumeration, Random, Collection, List, Properties, Map, Date } from '@java/java.util'
    import { Stream } from '@java/java.util.stream'
    import { ByteBuffer } from '@java/java.nio'
    import { CertPath, Certificate } from '@java/java.security.cert'
    import { AlgorithmParameterSpec, KeySpec } from '@java/java.security.spec'
    import { Enum, RuntimeException, Throwable, ClassLoader, SecurityException, Class, String, Exception, IllegalArgumentException } from '@java/java.lang'
    import { URI, URL } from '@java/java.net'
    import { Serializable, FilterInputStream, FilterOutputStream, File, InputStream, OutputStream } from '@java/java.io'
    import { CallbackHandler } from '@java/javax.security.auth.callback'
    import { Subject, Destroyable } from '@java/javax.security.auth'
    import { BiFunction, Function, BiConsumer } from '@java/java.util.function'
    import { Configuration } from '@java/javax.security.auth.login'
    export class AccessControlContext {
        constructor(arg0: ProtectionDomain[]);
        constructor(arg0: AccessControlContext, arg1: DomainCombiner);

        getDomainCombiner(): DomainCombiner;

        checkPermission(arg0: Permission): void;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class AccessControlException extends SecurityException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Permission);

        getPermission(): Permission;
    }

    export class AccessController {

        static doPrivileged<T extends Object>(arg0: PrivilegedAction<T>): T;

        static doPrivilegedWithCombiner<T extends Object>(arg0: PrivilegedAction<T>): T;

        static doPrivileged<T extends Object>(arg0: PrivilegedAction<T>, arg1: AccessControlContext): T;

        static doPrivileged<T extends Object>(arg0: PrivilegedAction<T>, arg1: AccessControlContext, arg2: Permission[]): T;

        static doPrivilegedWithCombiner<T extends Object>(arg0: PrivilegedAction<T>, arg1: AccessControlContext, arg2: Permission[]): T;

        static doPrivileged<T extends Object>(arg0: PrivilegedExceptionAction<T>): T;

        static doPrivilegedWithCombiner<T extends Object>(arg0: PrivilegedExceptionAction<T>): T;

        static doPrivileged<T extends Object>(arg0: PrivilegedExceptionAction<T>, arg1: AccessControlContext): T;

        static doPrivileged<T extends Object>(arg0: PrivilegedExceptionAction<T>, arg1: AccessControlContext, arg2: Permission[]): T;

        static doPrivilegedWithCombiner<T extends Object>(arg0: PrivilegedExceptionAction<T>, arg1: AccessControlContext, arg2: Permission[]): T;

        static getContext(): AccessControlContext;

        static checkPermission(arg0: Permission): void;
    }

    export interface AlgorithmConstraints {

        permits(arg0: Set<CryptoPrimitive>, arg1: String, arg2: AlgorithmParameters): boolean;

        permits(arg0: Set<CryptoPrimitive>, arg1: Key): boolean;

        permits(arg0: Set<CryptoPrimitive>, arg1: String, arg2: Key, arg3: AlgorithmParameters): boolean;
    }

    export class AlgorithmParameterGenerator {

        getAlgorithm(): String;

        static getInstance(arg0: String): AlgorithmParameterGenerator;

        static getInstance(arg0: String, arg1: String): AlgorithmParameterGenerator;

        static getInstance(arg0: String, arg1: Provider): AlgorithmParameterGenerator;

        getProvider(): Provider;

        init(arg0: number): void;

        init(arg0: number, arg1: SecureRandom): void;

        init(arg0: AlgorithmParameterSpec): void;

        init(arg0: AlgorithmParameterSpec, arg1: SecureRandom): void;

        generateParameters(): AlgorithmParameters;
    }

    export abstract class AlgorithmParameterGeneratorSpi {
        constructor();
    }

    export class AlgorithmParameters {

        getAlgorithm(): String;

        static getInstance(arg0: String): AlgorithmParameters;

        static getInstance(arg0: String, arg1: String): AlgorithmParameters;

        static getInstance(arg0: String, arg1: Provider): AlgorithmParameters;

        getProvider(): Provider;

        init(arg0: AlgorithmParameterSpec): void;

        init(arg0: number[]): void;

        init(arg0: number[], arg1: String): void;

        getParameterSpec<T extends AlgorithmParameterSpec>(arg0: Class<T>): T;

        getEncoded(): number[];

        getEncoded(arg0: String): number[];
        toString(): string;
    }

    export abstract class AlgorithmParametersSpi {
        constructor();
    }

    export class AllPermission extends Permission {
        constructor();
        constructor(arg0: String, arg1: String);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        newPermissionCollection(): PermissionCollection;
    }

    export interface AuthProvider { }
    export abstract class AuthProvider extends Provider {

        abstract login(arg0: Subject, arg1: CallbackHandler): void;

        abstract logout(): void;

        abstract setCallbackHandler(arg0: CallbackHandler): void;
    }

    export abstract class BasicPermission extends Permission implements Serializable {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        newPermissionCollection(): PermissionCollection;
    }

    export interface Certificate {

        getGuarantor(): Principal;

        getPrincipal(): Principal;

        getPublicKey(): PublicKey;

        encode(arg0: OutputStream): void;

        decode(arg0: InputStream): void;

        getFormat(): String;

        toString(arg0: boolean): String;
    }

    export class CodeSigner implements Serializable {
        constructor(arg0: CertPath, arg1: Timestamp);

        getSignerCertPath(): CertPath;

        getTimestamp(): Timestamp;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export class CodeSource implements Serializable {
        constructor(arg0: URL, arg1: Certificate[]);
        constructor(arg0: URL, arg1: CodeSigner[]);

        hashCode(): number;

        equals(arg0: Object): boolean;

        getLocation(): URL;

        getCertificates(): Certificate[];

        getCodeSigners(): CodeSigner[];

        implies(arg0: CodeSource): boolean;
        toString(): string;
    }

    export class CryptoPrimitive extends Enum<CryptoPrimitive> {
        static MESSAGE_DIGEST: CryptoPrimitive
        static SECURE_RANDOM: CryptoPrimitive
        static BLOCK_CIPHER: CryptoPrimitive
        static STREAM_CIPHER: CryptoPrimitive
        static MAC: CryptoPrimitive
        static KEY_WRAP: CryptoPrimitive
        static PUBLIC_KEY_ENCRYPTION: CryptoPrimitive
        static SIGNATURE: CryptoPrimitive
        static KEY_ENCAPSULATION: CryptoPrimitive
        static KEY_AGREEMENT: CryptoPrimitive

        static values(): CryptoPrimitive[];

        static valueOf(arg0: String): CryptoPrimitive;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class DigestException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class DigestInputStream extends FilterInputStream {
        constructor(arg0: InputStream, arg1: MessageDigest);

        getMessageDigest(): MessageDigest;

        setMessageDigest(arg0: MessageDigest): void;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        on(arg0: boolean): void;
        toString(): string;
    }

    export class DigestOutputStream extends FilterOutputStream {
        constructor(arg0: OutputStream, arg1: MessageDigest);

        getMessageDigest(): MessageDigest;

        setMessageDigest(arg0: MessageDigest): void;

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        on(arg0: boolean): void;
        toString(): string;
    }

    export interface DomainCombiner {

        combine(arg0: ProtectionDomain[], arg1: ProtectionDomain[]): ProtectionDomain[];
    }

    export class DomainLoadStoreParameter implements KeyStore.LoadStoreParameter {
        constructor(arg0: URI, arg1: Map<String, KeyStore.ProtectionParameter>);

        getConfiguration(): URI;

        getProtectionParams(): Map<String, KeyStore.ProtectionParameter>;

        getProtectionParameter(): KeyStore.ProtectionParameter;
    }

    export class DrbgParameters {

        static instantiation(arg0: number, arg1: DrbgParameters.Capability, arg2: number[]): DrbgParameters.Instantiation;

        static nextBytes(arg0: number, arg1: boolean, arg2: number[]): DrbgParameters.NextBytes;

        static reseed(arg0: boolean, arg1: number[]): DrbgParameters.Reseed;
    }
    export namespace DrbgParameters {
        export class Capability extends Enum<DrbgParameters.Capability> {
            static PR_AND_RESEED: DrbgParameters.Capability
            static RESEED_ONLY: DrbgParameters.Capability
            static NONE: DrbgParameters.Capability

            static values(): DrbgParameters.Capability[];

            static valueOf(arg0: String): DrbgParameters.Capability;
            toString(): string;

            supportsReseeding(): boolean;

            supportsPredictionResistance(): boolean;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Instantiation implements SecureRandomParameters {

            getStrength(): number;

            getCapability(): DrbgParameters.Capability;

            getPersonalizationString(): number[];
            toString(): string;
        }

        export class NextBytes implements SecureRandomParameters {

            getStrength(): number;

            getPredictionResistance(): boolean;

            getAdditionalInput(): number[];
        }

        export class Reseed implements SecureRandomParameters {

            getPredictionResistance(): boolean;

            getAdditionalInput(): number[];
        }

    }

    export class GeneralSecurityException extends Exception {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export interface Guard {

        checkGuard(arg0: Object): void;
    }

    export class GuardedObject implements Serializable {
        constructor(arg0: Object, arg1: Guard);

        getObject(): Object;
    }

    export interface Identity extends Principal, Serializable { }
    export abstract class Identity implements Principal, Serializable {
        constructor(arg0: String, arg1: IdentityScope);
        constructor(arg0: String);

        getName(): String;

        getScope(): IdentityScope;

        getPublicKey(): PublicKey;

        setPublicKey(arg0: PublicKey): void;

        setInfo(arg0: String): void;

        getInfo(): String;

        addCertificate(arg0: Certificate): void;

        removeCertificate(arg0: Certificate): void;

        certificates(): Certificate[];

        equals(arg0: Object): boolean;
        toString(): string;

        toString(arg0: boolean): String;

        hashCode(): number;
    }

    export interface IdentityScope { }
    export abstract class IdentityScope extends Identity {
        constructor(arg0: String);
        constructor(arg0: String, arg1: IdentityScope);

        static getSystemScope(): IdentityScope;

        abstract size(): number;

        abstract getIdentity(arg0: String): Identity;

        getIdentity(arg0: Principal): Identity;

        abstract getIdentity(arg0: PublicKey): Identity;

        abstract addIdentity(arg0: Identity): void;

        abstract removeIdentity(arg0: Identity): void;

        abstract identities(): Enumeration<Identity>;
        toString(): string;
    }

    export class InvalidAlgorithmParameterException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class InvalidKeyException extends KeyException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class InvalidParameterException extends IllegalArgumentException {
        constructor();
        constructor(arg0: String);
    }

    export namespace Key {
        const serialVersionUID: number
    }

    export interface Key extends Serializable {
        serialVersionUID: number

        getAlgorithm(): String;

        getFormat(): String;

        getEncoded(): number[];
    }

    export class KeyException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class KeyFactory {

        static getInstance(arg0: String): KeyFactory;

        static getInstance(arg0: String, arg1: String): KeyFactory;

        static getInstance(arg0: String, arg1: Provider): KeyFactory;

        getProvider(): Provider;

        getAlgorithm(): String;

        generatePublic(arg0: KeySpec): PublicKey;

        generatePrivate(arg0: KeySpec): PrivateKey;

        getKeySpec<T extends KeySpec>(arg0: Key, arg1: Class<T>): T;

        translateKey(arg0: Key): Key;
    }

    export abstract class KeyFactorySpi {
        constructor();
    }

    export class KeyManagementException extends KeyException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class KeyPair implements Serializable {
        constructor(arg0: PublicKey, arg1: PrivateKey);

        getPublic(): PublicKey;

        getPrivate(): PrivateKey;
    }

    export abstract class KeyPairGenerator extends KeyPairGeneratorSpi {

        getAlgorithm(): String;

        static getInstance(arg0: String): KeyPairGenerator;

        static getInstance(arg0: String, arg1: String): KeyPairGenerator;

        static getInstance(arg0: String, arg1: Provider): KeyPairGenerator;

        getProvider(): Provider;

        initialize(arg0: number): void;

        initialize(arg0: number, arg1: SecureRandom): void;

        initialize(arg0: AlgorithmParameterSpec): void;

        initialize(arg0: AlgorithmParameterSpec, arg1: SecureRandom): void;

        genKeyPair(): KeyPair;

        generateKeyPair(): KeyPair;
    }

    export abstract class KeyPairGeneratorSpi {
        constructor();

        abstract initialize(arg0: number, arg1: SecureRandom): void;

        initialize(arg0: AlgorithmParameterSpec, arg1: SecureRandom): void;

        abstract generateKeyPair(): KeyPair;
    }

    export class KeyRep implements Serializable {
        constructor(arg0: KeyRep.Type, arg1: String, arg2: String, arg3: number[]);
    }
    export namespace KeyRep {
        export class Type extends Enum<KeyRep.Type> {
            static SECRET: KeyRep.Type
            static PUBLIC: KeyRep.Type
            static PRIVATE: KeyRep.Type

            static values(): KeyRep.Type[];

            static valueOf(arg0: String): KeyRep.Type;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class KeyStore {

        static getInstance(arg0: String): KeyStore;

        static getInstance(arg0: String, arg1: String): KeyStore;

        static getInstance(arg0: String, arg1: Provider): KeyStore;

        static getDefaultType(): String;

        getProvider(): Provider;

        getType(): String;

        getKey(arg0: String, arg1: String[]): Key;

        getCertificateChain(arg0: String): Certificate[];

        getCertificate(arg0: String): Certificate;

        getCreationDate(arg0: String): Date;

        setKeyEntry(arg0: String, arg1: Key, arg2: String[], arg3: Certificate[]): void;

        setKeyEntry(arg0: String, arg1: number[], arg2: Certificate[]): void;

        setCertificateEntry(arg0: String, arg1: Certificate): void;

        deleteEntry(arg0: String): void;

        aliases(): Enumeration<String>;

        containsAlias(arg0: String): boolean;

        size(): number;

        isKeyEntry(arg0: String): boolean;

        isCertificateEntry(arg0: String): boolean;

        getCertificateAlias(arg0: Certificate): String;

        store(arg0: OutputStream, arg1: String[]): void;

        store(arg0: KeyStore.LoadStoreParameter): void;

        load(arg0: InputStream, arg1: String[]): void;

        load(arg0: KeyStore.LoadStoreParameter): void;

        getEntry(arg0: String, arg1: KeyStore.ProtectionParameter): KeyStore.Entry;

        setEntry(arg0: String, arg1: KeyStore.Entry, arg2: KeyStore.ProtectionParameter): void;

        entryInstanceOf(arg0: String, arg1: Class<KeyStore.Entry>): boolean;

        static getInstance(arg0: File, arg1: String[]): KeyStore;

        static getInstance(arg0: File, arg1: KeyStore.LoadStoreParameter): KeyStore;
    }
    export namespace KeyStore {
        export abstract class Builder {

            abstract getKeyStore(): KeyStore;

            abstract getProtectionParameter(arg0: String): KeyStore.ProtectionParameter;

            static newInstance(arg0: KeyStore, arg1: KeyStore.ProtectionParameter): KeyStore.Builder;

            static newInstance(arg0: String, arg1: Provider, arg2: File, arg3: KeyStore.ProtectionParameter): KeyStore.Builder;

            static newInstance(arg0: File, arg1: KeyStore.ProtectionParameter): KeyStore.Builder;

            static newInstance(arg0: String, arg1: Provider, arg2: KeyStore.ProtectionParameter): KeyStore.Builder;
        }

        export class CallbackHandlerProtection implements KeyStore.ProtectionParameter {
            constructor(arg0: CallbackHandler);

            getCallbackHandler(): CallbackHandler;
        }

        export interface Entry {

/* default */ getAttributes(): Set<KeyStore.Entry.Attribute>;
        }
        export namespace Entry {
            export interface Attribute {

                getName(): String;

                getValue(): String;
            }

        }

        export interface Attribute {

            getName(): String;

            getValue(): String;
        }

        export interface LoadStoreParameter {

            getProtectionParameter(): KeyStore.ProtectionParameter;
        }

        export class PasswordProtection implements KeyStore.ProtectionParameter, Destroyable {
            constructor(arg0: String[]);
            constructor(arg0: String[], arg1: String, arg2: AlgorithmParameterSpec);

            getProtectionAlgorithm(): String;

            getProtectionParameters(): AlgorithmParameterSpec;

            getPassword(): String[];

            destroy(): void;

            isDestroyed(): boolean;
        }

        export class PrivateKeyEntry implements KeyStore.Entry {
            constructor(arg0: PrivateKey, arg1: Certificate[]);
            constructor(arg0: PrivateKey, arg1: Certificate[], arg2: Set<KeyStore.Entry.Attribute>);

            getPrivateKey(): PrivateKey;

            getCertificateChain(): Certificate[];

            getCertificate(): Certificate;

            getAttributes(): Set<KeyStore.Entry.Attribute>;
            toString(): string;
        }

        export interface ProtectionParameter {
        }

        export class SecretKeyEntry implements KeyStore.Entry {
            constructor(arg0: SecretKey);
            constructor(arg0: SecretKey, arg1: Set<KeyStore.Entry.Attribute>);

            getSecretKey(): SecretKey;

            getAttributes(): Set<KeyStore.Entry.Attribute>;
            toString(): string;
        }

        export class TrustedCertificateEntry implements KeyStore.Entry {
            constructor(arg0: Certificate);
            constructor(arg0: Certificate, arg1: Set<KeyStore.Entry.Attribute>);

            getTrustedCertificate(): Certificate;

            getAttributes(): Set<KeyStore.Entry.Attribute>;
            toString(): string;
        }

    }

    export class KeyStoreException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export abstract class KeyStoreSpi {
        constructor();

        abstract engineGetKey(arg0: String, arg1: String[]): Key;

        abstract engineGetCertificateChain(arg0: String): Certificate[];

        abstract engineGetCertificate(arg0: String): Certificate;

        abstract engineGetCreationDate(arg0: String): Date;

        abstract engineSetKeyEntry(arg0: String, arg1: Key, arg2: String[], arg3: Certificate[]): void;

        abstract engineSetKeyEntry(arg0: String, arg1: number[], arg2: Certificate[]): void;

        abstract engineSetCertificateEntry(arg0: String, arg1: Certificate): void;

        abstract engineDeleteEntry(arg0: String): void;

        abstract engineAliases(): Enumeration<String>;

        abstract engineContainsAlias(arg0: String): boolean;

        abstract engineSize(): number;

        abstract engineIsKeyEntry(arg0: String): boolean;

        abstract engineIsCertificateEntry(arg0: String): boolean;

        abstract engineGetCertificateAlias(arg0: Certificate): String;

        abstract engineStore(arg0: OutputStream, arg1: String[]): void;

        engineStore(arg0: KeyStore.LoadStoreParameter): void;

        abstract engineLoad(arg0: InputStream, arg1: String[]): void;

        engineLoad(arg0: KeyStore.LoadStoreParameter): void;

        engineGetEntry(arg0: String, arg1: KeyStore.ProtectionParameter): KeyStore.Entry;

        engineSetEntry(arg0: String, arg1: KeyStore.Entry, arg2: KeyStore.ProtectionParameter): void;

        engineEntryInstanceOf(arg0: String, arg1: Class<KeyStore.Entry>): boolean;

        engineProbe(arg0: InputStream): boolean;
    }

    export abstract class MessageDigest extends MessageDigestSpi {

        static getInstance(arg0: String): MessageDigest;

        static getInstance(arg0: String, arg1: String): MessageDigest;

        static getInstance(arg0: String, arg1: Provider): MessageDigest;

        getProvider(): Provider;

        update(arg0: number): void;

        update(arg0: number[], arg1: number, arg2: number): void;

        update(arg0: number[]): void;

        update(arg0: ByteBuffer): void;

        digest(): number[];

        digest(arg0: number[], arg1: number, arg2: number): number;

        digest(arg0: number[]): number[];
        toString(): string;

        static isEqual(arg0: number[], arg1: number[]): boolean;

        reset(): void;

        getAlgorithm(): String;

        getDigestLength(): number;

        clone(): Object;
    }

    export abstract class MessageDigestSpi {
        constructor();

        clone(): Object;
    }

    export class NoSuchAlgorithmException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class NoSuchProviderException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
    }

    export class PKCS12Attribute implements KeyStore.Entry.Attribute {
        constructor(arg0: String, arg1: String);
        constructor(arg0: number[]);

        getName(): String;

        getValue(): String;

        getEncoded(): number[];

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export abstract class Permission implements Guard, Serializable {
        constructor(arg0: String);

        checkGuard(arg0: Object): void;

        abstract implies(arg0: Permission): boolean;

        abstract equals(arg0: Object): boolean;

        abstract hashCode(): number;

        getName(): String;

        abstract getActions(): String;

        newPermissionCollection(): PermissionCollection;
        toString(): string;
    }

    export abstract class PermissionCollection implements Serializable {
        constructor();

        abstract add(arg0: Permission): void;

        abstract implies(arg0: Permission): boolean;

        abstract elements(): Enumeration<Permission>;

        elementsAsStream(): Stream<Permission>;

        setReadOnly(): void;

        isReadOnly(): boolean;
        toString(): string;
    }

    export class Permissions extends PermissionCollection implements Serializable {
        constructor();

        add(arg0: Permission): void;

        implies(arg0: Permission): boolean;

        elements(): Enumeration<Permission>;
    }

    export abstract class Policy {
        static UNSUPPORTED_EMPTY_COLLECTION: PermissionCollection
        constructor();

        static getPolicy(): Policy;

        static setPolicy(arg0: Policy): void;

        static getInstance(arg0: String, arg1: Policy.Parameters): Policy;

        static getInstance(arg0: String, arg1: Policy.Parameters, arg2: String): Policy;

        static getInstance(arg0: String, arg1: Policy.Parameters, arg2: Provider): Policy;

        getProvider(): Provider;

        getType(): String;

        getParameters(): Policy.Parameters;

        getPermissions(arg0: CodeSource): PermissionCollection;

        getPermissions(arg0: ProtectionDomain): PermissionCollection;

        implies(arg0: ProtectionDomain, arg1: Permission): boolean;

        refresh(): void;
    }
    export namespace Policy {
        export interface Parameters {
        }

    }

    export abstract class PolicySpi {
        constructor();
    }

    export interface Principal {

        equals(arg0: Object): boolean;
        toString(): string;

        hashCode(): number;

        getName(): String;

/* default */ implies(arg0: Subject): boolean;
    }

    export namespace PrivateKey {
        const serialVersionUID: number
    }

    export interface PrivateKey extends Key, Destroyable {
        serialVersionUID: number
    }

    export interface PrivilegedAction<T extends Object> extends Object {

        run(): T;
    }

    export class PrivilegedActionException extends Exception {
        constructor(arg0: Exception);

        getException(): Exception;
        toString(): string;
    }

    export interface PrivilegedExceptionAction<T extends Object> extends Object {

        run(): T;
    }

    export class ProtectionDomain {
        constructor(arg0: CodeSource, arg1: PermissionCollection);
        constructor(arg0: CodeSource, arg1: PermissionCollection, arg2: ClassLoader, arg3: Principal[]);

        getCodeSource(): CodeSource;

        getClassLoader(): ClassLoader;

        getPrincipals(): Principal[];

        getPermissions(): PermissionCollection;

        staticPermissionsOnly(): boolean;

        implies(arg0: Permission): boolean;
        toString(): string;
    }

    export abstract class Provider extends Properties {

        configure(arg0: String): Provider;

        isConfigured(): boolean;

        getName(): String;

        getVersion(): number;

        getVersionStr(): String;

        getInfo(): String;
        toString(): string;

        clear(): void;

        load(arg0: InputStream): void;

        putAll(arg0: Map<any, any>): void;

        entrySet(): Set<Map.Entry<Object, Object>>;

        keySet(): Set<Object>;

        values(): Collection<Object>;

        put(arg0: Object, arg1: Object): Object;

        putIfAbsent(arg0: Object, arg1: Object): Object;

        remove(arg0: Object): Object;

        remove(arg0: Object, arg1: Object): boolean;

        replace(arg0: Object, arg1: Object, arg2: Object): boolean;

        replace(arg0: Object, arg1: Object): Object;

        replaceAll(arg0: BiFunction<Object, Object, Object>): void;

        compute(arg0: Object, arg1: BiFunction<Object, Object, Object>): Object;

        computeIfAbsent(arg0: Object, arg1: Function<Object, Object>): Object;

        computeIfPresent(arg0: Object, arg1: BiFunction<Object, Object, Object>): Object;

        merge(arg0: Object, arg1: Object, arg2: BiFunction<Object, Object, Object>): Object;

        get(arg0: Object): Object;

        getOrDefault(arg0: Object, arg1: Object): Object;

        forEach(arg0: BiConsumer<Object, Object>): void;

        keys(): Enumeration<Object>;

        elements(): Enumeration<Object>;

        getProperty(arg0: String): String;

        getService(arg0: String, arg1: String): Provider.Service;

        getServices(): Set<Provider.Service>;
    }
    export namespace Provider {
        export class Service {
            constructor(arg0: Provider, arg1: String, arg2: String, arg3: String, arg4: List<String>, arg5: Map<String, String>);

            getType(): String;

            getAlgorithm(): String;

            getProvider(): Provider;

            getClassName(): String;

            getAttribute(arg0: String): String;

            newInstance(arg0: Object): Object;

            supportsParameter(arg0: Object): boolean;
            toString(): string;
        }

    }

    export class ProviderException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export namespace PublicKey {
        const serialVersionUID: number
    }

    export interface PublicKey extends Key {
        serialVersionUID: number
    }

    export class SecureClassLoader extends ClassLoader {
    }

    export interface SecureRandom { }
    export class SecureRandom extends Random {
        constructor();
        constructor(arg0: number[]);

        static getInstance(arg0: String): SecureRandom;

        static getInstance(arg0: String, arg1: String): SecureRandom;

        static getInstance(arg0: String, arg1: Provider): SecureRandom;

        static getInstance(arg0: String, arg1: SecureRandomParameters): SecureRandom;

        static getInstance(arg0: String, arg1: SecureRandomParameters, arg2: String): SecureRandom;

        static getInstance(arg0: String, arg1: SecureRandomParameters, arg2: Provider): SecureRandom;

        getProvider(): Provider;

        getAlgorithm(): String;
        toString(): string;

        getParameters(): SecureRandomParameters;

        setSeed(arg0: number[]): void;

        setSeed(arg0: number): void;

        nextBytes(arg0: number[]): void;

        nextBytes(arg0: number[], arg1: SecureRandomParameters): void;

        static getSeed(arg0: number): number[];

        generateSeed(arg0: number): number[];

        static getInstanceStrong(): SecureRandom;

        reseed(): void;

        reseed(arg0: SecureRandomParameters): void;
    }

    export interface SecureRandomParameters {
    }

    export abstract class SecureRandomSpi implements Serializable {
        constructor();
        toString(): string;
    }

    export class Security {

        static getAlgorithmProperty(arg0: String, arg1: String): String;

        static insertProviderAt(arg0: Provider, arg1: number): number;

        static addProvider(arg0: Provider): number;

        static removeProvider(arg0: String): void;

        static getProviders(): Provider[];

        static getProvider(arg0: String): Provider;

        static getProviders(arg0: String): Provider[];

        static getProviders(arg0: Map<String, String>): Provider[];

        static getProperty(arg0: String): String;

        static setProperty(arg0: String, arg1: String): void;

        static getAlgorithms(arg0: String): Set<String>;
    }

    export class SecurityPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export abstract class Signature extends SignatureSpi {

        static getInstance(arg0: String): Signature;

        static getInstance(arg0: String, arg1: String): Signature;

        static getInstance(arg0: String, arg1: Provider): Signature;

        getProvider(): Provider;

        initVerify(arg0: PublicKey): void;

        initVerify(arg0: Certificate): void;

        initSign(arg0: PrivateKey): void;

        initSign(arg0: PrivateKey, arg1: SecureRandom): void;

        sign(): number[];

        sign(arg0: number[], arg1: number, arg2: number): number;

        verify(arg0: number[]): boolean;

        verify(arg0: number[], arg1: number, arg2: number): boolean;

        update(arg0: number): void;

        update(arg0: number[]): void;

        update(arg0: number[], arg1: number, arg2: number): void;

        update(arg0: ByteBuffer): void;

        getAlgorithm(): String;
        toString(): string;

        setParameter(arg0: String, arg1: Object): void;

        setParameter(arg0: AlgorithmParameterSpec): void;

        getParameters(): AlgorithmParameters;

        getParameter(arg0: String): Object;

        clone(): Object;
    }

    export class SignatureException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export abstract class SignatureSpi {
        constructor();

        clone(): Object;
    }

    export class SignedObject implements Serializable {
        constructor(arg0: Serializable, arg1: PrivateKey, arg2: Signature);

        getObject(): Object;

        getSignature(): number[];

        getAlgorithm(): String;

        verify(arg0: PublicKey, arg1: Signature): boolean;
    }

    export interface Signer { }
    export abstract class Signer extends Identity {
        constructor(arg0: String);
        constructor(arg0: String, arg1: IdentityScope);

        getPrivateKey(): PrivateKey;

        setKeyPair(arg0: KeyPair): void;
        toString(): string;
    }

    export class Timestamp implements Serializable {
        constructor(arg0: Date, arg1: CertPath);

        getTimestamp(): Date;

        getSignerCertPath(): CertPath;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export class URIParameter implements Policy.Parameters, Configuration.Parameters {
        constructor(arg0: URI);

        getURI(): URI;
    }

    export class UnrecoverableEntryException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
    }

    export class UnrecoverableKeyException extends UnrecoverableEntryException {
        constructor();
        constructor(arg0: String);
    }

    export class UnresolvedPermission extends Permission implements Serializable {
        constructor(arg0: String, arg1: String, arg2: String, arg3: Certificate[]);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        getUnresolvedType(): String;

        getUnresolvedName(): String;

        getUnresolvedActions(): String;

        getUnresolvedCerts(): Certificate[];
        toString(): string;

        newPermissionCollection(): PermissionCollection;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="javax.security.auth.x500.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.math.d.ts" />
declare module '@java/java.security.cert' {
    import { KeyStore, PublicKey, Principal, GeneralSecurityException, Provider } from '@java/java.security'
    import { Enum, Throwable, Class, Cloneable, String } from '@java/java.lang'
    import { Iterator, Collection, List, Set, Map, Date } from '@java/java.util'
    import { X500Principal } from '@java/javax.security.auth.x500'
    import { URI } from '@java/java.net'
    import { Serializable, InputStream, OutputStream } from '@java/java.io'
    import { BigInteger } from '@java/java.math'
    export abstract class CRL {

        getType(): String;
        toString(): string;

        abstract isRevoked(arg0: Certificate): boolean;
    }

    export class CRLException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class CRLReason extends Enum<CRLReason> {
        static UNSPECIFIED: CRLReason
        static KEY_COMPROMISE: CRLReason
        static CA_COMPROMISE: CRLReason
        static AFFILIATION_CHANGED: CRLReason
        static SUPERSEDED: CRLReason
        static CESSATION_OF_OPERATION: CRLReason
        static CERTIFICATE_HOLD: CRLReason
        static UNUSED: CRLReason
        static REMOVE_FROM_CRL: CRLReason
        static PRIVILEGE_WITHDRAWN: CRLReason
        static AA_COMPROMISE: CRLReason

        static values(): CRLReason[];

        static valueOf(arg0: String): CRLReason;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface CRLSelector extends Cloneable {

        match(arg0: CRL): boolean;

        clone(): Object;
    }

    export abstract class CertPath implements Serializable {

        getType(): String;

        abstract getEncodings(): Iterator<String>;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        abstract getEncoded(): number[];

        abstract getEncoded(arg0: String): number[];

        abstract getCertificates(): List<Certificate>;
    }
    export namespace CertPath {
        export class CertPathRep implements Serializable {
        }

    }

    export class CertPathBuilder {

        static getInstance(arg0: String): CertPathBuilder;

        static getInstance(arg0: String, arg1: String): CertPathBuilder;

        static getInstance(arg0: String, arg1: Provider): CertPathBuilder;

        getProvider(): Provider;

        getAlgorithm(): String;

        build(arg0: CertPathParameters): CertPathBuilderResult;

        static getDefaultType(): String;

        getRevocationChecker(): CertPathChecker;
    }

    export class CertPathBuilderException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export interface CertPathBuilderResult extends Cloneable {

        getCertPath(): CertPath;

        clone(): Object;
    }

    export abstract class CertPathBuilderSpi {
        constructor();

        abstract engineBuild(arg0: CertPathParameters): CertPathBuilderResult;

        engineGetRevocationChecker(): CertPathChecker;
    }

    export interface CertPathChecker {

        init(arg0: boolean): void;

        isForwardCheckingSupported(): boolean;

        check(arg0: Certificate): void;
    }

    export interface CertPathParameters extends Cloneable {

        clone(): Object;
    }

    export class CertPathValidator {

        static getInstance(arg0: String): CertPathValidator;

        static getInstance(arg0: String, arg1: String): CertPathValidator;

        static getInstance(arg0: String, arg1: Provider): CertPathValidator;

        getProvider(): Provider;

        getAlgorithm(): String;

        validate(arg0: CertPath, arg1: CertPathParameters): CertPathValidatorResult;

        static getDefaultType(): String;

        getRevocationChecker(): CertPathChecker;
    }

    export class CertPathValidatorException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: String, arg1: Throwable, arg2: CertPath, arg3: number);
        constructor(arg0: String, arg1: Throwable, arg2: CertPath, arg3: number, arg4: CertPathValidatorException.Reason);

        getCertPath(): CertPath;

        getIndex(): number;

        getReason(): CertPathValidatorException.Reason;
    }
    export namespace CertPathValidatorException {
        export class BasicReason extends Enum<CertPathValidatorException.BasicReason> implements CertPathValidatorException.Reason {
            static UNSPECIFIED: CertPathValidatorException.BasicReason
            static EXPIRED: CertPathValidatorException.BasicReason
            static NOT_YET_VALID: CertPathValidatorException.BasicReason
            static REVOKED: CertPathValidatorException.BasicReason
            static UNDETERMINED_REVOCATION_STATUS: CertPathValidatorException.BasicReason
            static INVALID_SIGNATURE: CertPathValidatorException.BasicReason
            static ALGORITHM_CONSTRAINED: CertPathValidatorException.BasicReason

            static values(): CertPathValidatorException.BasicReason[];

            static valueOf(arg0: String): CertPathValidatorException.BasicReason;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export interface Reason extends Serializable {
        }

    }

    export interface CertPathValidatorResult extends Cloneable {

        clone(): Object;
    }

    export abstract class CertPathValidatorSpi {
        constructor();

        abstract engineValidate(arg0: CertPath, arg1: CertPathParameters): CertPathValidatorResult;

        engineGetRevocationChecker(): CertPathChecker;
    }

    export interface CertSelector extends Cloneable {

        match(arg0: Certificate): boolean;

        clone(): Object;
    }

    export class CertStore {

        getCertificates(arg0: CertSelector): Collection<Certificate>;

        getCRLs(arg0: CRLSelector): Collection<CRL>;

        static getInstance(arg0: String, arg1: CertStoreParameters): CertStore;

        static getInstance(arg0: String, arg1: CertStoreParameters, arg2: String): CertStore;

        static getInstance(arg0: String, arg1: CertStoreParameters, arg2: Provider): CertStore;

        getCertStoreParameters(): CertStoreParameters;

        getType(): String;

        getProvider(): Provider;

        static getDefaultType(): String;
    }

    export class CertStoreException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export interface CertStoreParameters extends Cloneable {

        clone(): Object;
    }

    export abstract class CertStoreSpi {
        constructor(arg0: CertStoreParameters);

        abstract engineGetCertificates(arg0: CertSelector): Collection<Certificate>;

        abstract engineGetCRLs(arg0: CRLSelector): Collection<CRL>;
    }

    export abstract class Certificate implements Serializable {

        getType(): String;

        equals(arg0: Object): boolean;

        hashCode(): number;

        abstract getEncoded(): number[];

        abstract verify(arg0: PublicKey): void;

        abstract verify(arg0: PublicKey, arg1: String): void;

        verify(arg0: PublicKey, arg1: Provider): void;
        toString(): string;

        abstract getPublicKey(): PublicKey;
    }
    export namespace Certificate {
        export class CertificateRep implements Serializable {
        }

    }

    export class CertificateEncodingException extends CertificateException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class CertificateException extends GeneralSecurityException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class CertificateExpiredException extends CertificateException {
        constructor();
        constructor(arg0: String);
    }

    export class CertificateFactory {

        static getInstance(arg0: String): CertificateFactory;

        static getInstance(arg0: String, arg1: String): CertificateFactory;

        static getInstance(arg0: String, arg1: Provider): CertificateFactory;

        getProvider(): Provider;

        getType(): String;

        generateCertificate(arg0: InputStream): Certificate;

        getCertPathEncodings(): Iterator<String>;

        generateCertPath(arg0: InputStream): CertPath;

        generateCertPath(arg0: InputStream, arg1: String): CertPath;

        generateCertPath(arg0: List<Certificate>): CertPath;

        generateCertificates(arg0: InputStream): Collection<Certificate>;

        generateCRL(arg0: InputStream): CRL;

        generateCRLs(arg0: InputStream): Collection<CRL>;
    }

    export abstract class CertificateFactorySpi {
        constructor();

        abstract engineGenerateCertificate(arg0: InputStream): Certificate;

        engineGenerateCertPath(arg0: InputStream): CertPath;

        engineGenerateCertPath(arg0: InputStream, arg1: String): CertPath;

        engineGenerateCertPath(arg0: List<Certificate>): CertPath;

        engineGetCertPathEncodings(): Iterator<String>;

        abstract engineGenerateCertificates(arg0: InputStream): Collection<Certificate>;

        abstract engineGenerateCRL(arg0: InputStream): CRL;

        abstract engineGenerateCRLs(arg0: InputStream): Collection<CRL>;
    }

    export class CertificateNotYetValidException extends CertificateException {
        constructor();
        constructor(arg0: String);
    }

    export class CertificateParsingException extends CertificateException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class CertificateRevokedException extends CertificateException {
        constructor(arg0: Date, arg1: CRLReason, arg2: X500Principal, arg3: Map<String, Extension>);

        getRevocationDate(): Date;

        getRevocationReason(): CRLReason;

        getAuthorityName(): X500Principal;

        getInvalidityDate(): Date;

        getExtensions(): Map<String, Extension>;

        getMessage(): String;
    }

    export class CollectionCertStoreParameters implements CertStoreParameters {
        constructor(arg0: Collection<any>);
        constructor();

        getCollection(): Collection<any>;

        clone(): Object;
        toString(): string;
    }

    export interface Extension {

        getId(): String;

        isCritical(): boolean;

        getValue(): number[];

        encode(arg0: OutputStream): void;
    }

    export class LDAPCertStoreParameters implements CertStoreParameters {
        constructor(arg0: String, arg1: number);
        constructor(arg0: String);
        constructor();

        getServerName(): String;

        getPort(): number;

        clone(): Object;
        toString(): string;
    }

    export class PKIXBuilderParameters extends PKIXParameters {
        constructor(arg0: Set<TrustAnchor>, arg1: CertSelector);
        constructor(arg0: KeyStore, arg1: CertSelector);

        setMaxPathLength(arg0: number): void;

        getMaxPathLength(): number;
        toString(): string;
    }

    export class PKIXCertPathBuilderResult extends PKIXCertPathValidatorResult implements CertPathBuilderResult {
        constructor(arg0: CertPath, arg1: TrustAnchor, arg2: PolicyNode, arg3: PublicKey);

        getCertPath(): CertPath;
        toString(): string;
    }

    export abstract class PKIXCertPathChecker implements CertPathChecker, Cloneable {

        abstract init(arg0: boolean): void;

        abstract isForwardCheckingSupported(): boolean;

        abstract getSupportedExtensions(): Set<String>;

        abstract check(arg0: Certificate, arg1: Collection<String>): void;

        check(arg0: Certificate): void;

        clone(): Object;
    }

    export class PKIXCertPathValidatorResult implements CertPathValidatorResult {
        constructor(arg0: TrustAnchor, arg1: PolicyNode, arg2: PublicKey);

        getTrustAnchor(): TrustAnchor;

        getPolicyTree(): PolicyNode;

        getPublicKey(): PublicKey;

        clone(): Object;
        toString(): string;
    }

    export class PKIXParameters implements CertPathParameters {
        constructor(arg0: Set<TrustAnchor>);
        constructor(arg0: KeyStore);

        getTrustAnchors(): Set<TrustAnchor>;

        setTrustAnchors(arg0: Set<TrustAnchor>): void;

        getInitialPolicies(): Set<String>;

        setInitialPolicies(arg0: Set<String>): void;

        setCertStores(arg0: List<CertStore>): void;

        addCertStore(arg0: CertStore): void;

        getCertStores(): List<CertStore>;

        setRevocationEnabled(arg0: boolean): void;

        isRevocationEnabled(): boolean;

        setExplicitPolicyRequired(arg0: boolean): void;

        isExplicitPolicyRequired(): boolean;

        setPolicyMappingInhibited(arg0: boolean): void;

        isPolicyMappingInhibited(): boolean;

        setAnyPolicyInhibited(arg0: boolean): void;

        isAnyPolicyInhibited(): boolean;

        setPolicyQualifiersRejected(arg0: boolean): void;

        getPolicyQualifiersRejected(): boolean;

        getDate(): Date;

        setDate(arg0: Date): void;

        setCertPathCheckers(arg0: List<PKIXCertPathChecker>): void;

        getCertPathCheckers(): List<PKIXCertPathChecker>;

        addCertPathChecker(arg0: PKIXCertPathChecker): void;

        getSigProvider(): String;

        setSigProvider(arg0: String): void;

        getTargetCertConstraints(): CertSelector;

        setTargetCertConstraints(arg0: CertSelector): void;

        clone(): Object;
        toString(): string;
    }

    export class PKIXReason extends Enum<PKIXReason> implements CertPathValidatorException.Reason {
        static NAME_CHAINING: PKIXReason
        static INVALID_KEY_USAGE: PKIXReason
        static INVALID_POLICY: PKIXReason
        static NO_TRUST_ANCHOR: PKIXReason
        static UNRECOGNIZED_CRIT_EXT: PKIXReason
        static NOT_CA_CERT: PKIXReason
        static PATH_TOO_LONG: PKIXReason
        static INVALID_NAME: PKIXReason

        static values(): PKIXReason[];

        static valueOf(arg0: String): PKIXReason;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export abstract class PKIXRevocationChecker extends PKIXCertPathChecker {

        setOcspResponder(arg0: URI): void;

        getOcspResponder(): URI;

        setOcspResponderCert(arg0: X509Certificate): void;

        getOcspResponderCert(): X509Certificate;

        setOcspExtensions(arg0: List<Extension>): void;

        getOcspExtensions(): List<Extension>;

        setOcspResponses(arg0: Map<X509Certificate, number[]>): void;

        getOcspResponses(): Map<X509Certificate, number[]>;

        setOptions(arg0: Set<PKIXRevocationChecker.Option>): void;

        getOptions(): Set<PKIXRevocationChecker.Option>;

        abstract getSoftFailExceptions(): List<CertPathValidatorException>;

        clone(): PKIXRevocationChecker;
    }
    export namespace PKIXRevocationChecker {
        export class Option extends Enum<PKIXRevocationChecker.Option> {
            static ONLY_END_ENTITY: PKIXRevocationChecker.Option
            static PREFER_CRLS: PKIXRevocationChecker.Option
            static NO_FALLBACK: PKIXRevocationChecker.Option
            static SOFT_FAIL: PKIXRevocationChecker.Option

            static values(): PKIXRevocationChecker.Option[];

            static valueOf(arg0: String): PKIXRevocationChecker.Option;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export interface PolicyNode {

        getParent(): PolicyNode;

        getChildren(): Iterator<PolicyNode>;

        getDepth(): number;

        getValidPolicy(): String;

        getPolicyQualifiers(): Set<PolicyQualifierInfo>;

        getExpectedPolicies(): Set<String>;

        isCritical(): boolean;
    }

    export class PolicyQualifierInfo {
        constructor(arg0: number[]);

        getPolicyQualifierId(): String;

        getEncoded(): number[];

        getPolicyQualifier(): number[];
        toString(): string;
    }

    export class TrustAnchor {
        constructor(arg0: X509Certificate, arg1: number[]);
        constructor(arg0: X500Principal, arg1: PublicKey, arg2: number[]);
        constructor(arg0: String, arg1: PublicKey, arg2: number[]);

        getTrustedCert(): X509Certificate;

        getCA(): X500Principal;

        getCAName(): String;

        getCAPublicKey(): PublicKey;

        getNameConstraints(): number[];
        toString(): string;
    }

    export class URICertStoreParameters implements CertStoreParameters {
        constructor(arg0: URI);

        getURI(): URI;

        clone(): URICertStoreParameters;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export abstract class X509CRL extends CRL implements X509Extension {

        equals(arg0: Object): boolean;

        hashCode(): number;

        abstract getEncoded(): number[];

        abstract verify(arg0: PublicKey): void;

        abstract verify(arg0: PublicKey, arg1: String): void;

        verify(arg0: PublicKey, arg1: Provider): void;

        abstract getVersion(): number;

        abstract getIssuerDN(): Principal;

        getIssuerX500Principal(): X500Principal;

        abstract getThisUpdate(): Date;

        abstract getNextUpdate(): Date;

        abstract getRevokedCertificate(arg0: BigInteger): X509CRLEntry;

        getRevokedCertificate(arg0: X509Certificate): X509CRLEntry;

        abstract getRevokedCertificates(): Set<X509CRLEntry>;

        abstract getTBSCertList(): number[];

        abstract getSignature(): number[];

        abstract getSigAlgName(): String;

        abstract getSigAlgOID(): String;

        abstract getSigAlgParams(): number[];
    }

    export abstract class X509CRLEntry implements X509Extension {
        constructor();

        equals(arg0: Object): boolean;

        hashCode(): number;

        abstract getEncoded(): number[];

        abstract getSerialNumber(): BigInteger;

        getCertificateIssuer(): X500Principal;

        abstract getRevocationDate(): Date;

        abstract hasExtensions(): boolean;
        toString(): string;

        getRevocationReason(): CRLReason;
    }

    export class X509CRLSelector implements CRLSelector {
        constructor();

        setIssuers(arg0: Collection<X500Principal>): void;

        setIssuerNames(arg0: Collection<any>): void;

        addIssuer(arg0: X500Principal): void;

        addIssuerName(arg0: String): void;

        addIssuerName(arg0: number[]): void;

        setMinCRLNumber(arg0: BigInteger): void;

        setMaxCRLNumber(arg0: BigInteger): void;

        setDateAndTime(arg0: Date): void;

        setCertificateChecking(arg0: X509Certificate): void;

        getIssuers(): Collection<X500Principal>;

        getIssuerNames(): Collection<Object>;

        getMinCRL(): BigInteger;

        getMaxCRL(): BigInteger;

        getDateAndTime(): Date;

        getCertificateChecking(): X509Certificate;
        toString(): string;

        match(arg0: CRL): boolean;

        clone(): Object;
    }

    export class X509CertSelector implements CertSelector {
        constructor();

        setCertificate(arg0: X509Certificate): void;

        setSerialNumber(arg0: BigInteger): void;

        setIssuer(arg0: X500Principal): void;

        setIssuer(arg0: String): void;

        setIssuer(arg0: number[]): void;

        setSubject(arg0: X500Principal): void;

        setSubject(arg0: String): void;

        setSubject(arg0: number[]): void;

        setSubjectKeyIdentifier(arg0: number[]): void;

        setAuthorityKeyIdentifier(arg0: number[]): void;

        setCertificateValid(arg0: Date): void;

        setPrivateKeyValid(arg0: Date): void;

        setSubjectPublicKeyAlgID(arg0: String): void;

        setSubjectPublicKey(arg0: PublicKey): void;

        setSubjectPublicKey(arg0: number[]): void;

        setKeyUsage(arg0: boolean[]): void;

        setExtendedKeyUsage(arg0: Set<String>): void;

        setMatchAllSubjectAltNames(arg0: boolean): void;

        setSubjectAlternativeNames(arg0: Collection<List<any>>): void;

        addSubjectAlternativeName(arg0: number, arg1: String): void;

        addSubjectAlternativeName(arg0: number, arg1: number[]): void;

        setNameConstraints(arg0: number[]): void;

        setBasicConstraints(arg0: number): void;

        setPolicy(arg0: Set<String>): void;

        setPathToNames(arg0: Collection<List<any>>): void;

        addPathToName(arg0: number, arg1: String): void;

        addPathToName(arg0: number, arg1: number[]): void;

        getCertificate(): X509Certificate;

        getSerialNumber(): BigInteger;

        getIssuer(): X500Principal;

        getIssuerAsString(): String;

        getIssuerAsBytes(): number[];

        getSubject(): X500Principal;

        getSubjectAsString(): String;

        getSubjectAsBytes(): number[];

        getSubjectKeyIdentifier(): number[];

        getAuthorityKeyIdentifier(): number[];

        getCertificateValid(): Date;

        getPrivateKeyValid(): Date;

        getSubjectPublicKeyAlgID(): String;

        getSubjectPublicKey(): PublicKey;

        getKeyUsage(): boolean[];

        getExtendedKeyUsage(): Set<String>;

        getMatchAllSubjectAltNames(): boolean;

        getSubjectAlternativeNames(): Collection<List<any>>;

        getNameConstraints(): number[];

        getBasicConstraints(): number;

        getPolicy(): Set<String>;

        getPathToNames(): Collection<List<any>>;
        toString(): string;

        match(arg0: Certificate): boolean;

        clone(): Object;
    }

    export abstract class X509Certificate extends Certificate implements X509Extension {

        abstract checkValidity(): void;

        abstract checkValidity(arg0: Date): void;

        abstract getVersion(): number;

        abstract getSerialNumber(): BigInteger;

        abstract getIssuerDN(): Principal;

        getIssuerX500Principal(): X500Principal;

        abstract getSubjectDN(): Principal;

        getSubjectX500Principal(): X500Principal;

        abstract getNotBefore(): Date;

        abstract getNotAfter(): Date;

        abstract getTBSCertificate(): number[];

        abstract getSignature(): number[];

        abstract getSigAlgName(): String;

        abstract getSigAlgOID(): String;

        abstract getSigAlgParams(): number[];

        abstract getIssuerUniqueID(): boolean[];

        abstract getSubjectUniqueID(): boolean[];

        abstract getKeyUsage(): boolean[];

        getExtendedKeyUsage(): List<String>;

        abstract getBasicConstraints(): number;

        getSubjectAlternativeNames(): Collection<List<any>>;

        getIssuerAlternativeNames(): Collection<List<any>>;

        verify(arg0: PublicKey, arg1: Provider): void;
    }

    export interface X509Extension {

        hasUnsupportedCriticalExtension(): boolean;

        getCriticalExtensionOIDs(): Set<String>;

        getNonCriticalExtensionOIDs(): Set<String>;

        getExtensionValue(arg0: String): number[];
    }

}
/// <reference path="java.rmi.d.ts" />
/// <reference path="java.lang.reflect.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.rmi.server' {
    import { RemoteException, Remote } from '@java/java.rmi'
    import { InvocationHandler, Method } from '@java/java.lang.reflect'
    import { ClassLoader, Throwable, Class, String, Exception, CloneNotSupportedException } from '@java/java.lang'
    import { URL, Socket, ServerSocket } from '@java/java.net'
    import { DataInput, PrintStream, DataOutput, Serializable, ObjectOutput, ObjectInputFilter, OutputStream, Externalizable, ObjectInput } from '@java/java.io'
    export class ExportException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export namespace LoaderHandler {
        const packagePrefix: String
    }

    export interface LoaderHandler {
        packagePrefix: String

        loadClass(arg0: String): Class<any>;

        loadClass(arg0: URL, arg1: String): Class<any>;

        getSecurityContext(arg0: ClassLoader): Object;
    }

    export class LogStream extends PrintStream {
        static SILENT: number
        static BRIEF: number
        static VERBOSE: number

        static log(arg0: String): LogStream;

        static getDefaultStream(): PrintStream;

        static setDefaultStream(arg0: PrintStream): void;

        getOutputStream(): OutputStream;

        setOutputStream(arg0: OutputStream): void;

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;
        toString(): string;

        static parseLevel(arg0: String): number;
    }

    export class ObjID implements Serializable {
        static REGISTRY_ID: number
        static ACTIVATOR_ID: number
        static DGC_ID: number
        constructor();
        constructor(arg0: number);

        write(arg0: ObjectOutput): void;

        static read(arg0: ObjectInput): ObjID;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export class Operation {
        constructor(arg0: String);

        getOperation(): String;
        toString(): string;
    }

    export class RMIClassLoader {

        static loadClass(arg0: String): Class<any>;

        static loadClass(arg0: URL, arg1: String): Class<any>;

        static loadClass(arg0: String, arg1: String): Class<any>;

        static loadClass(arg0: String, arg1: String, arg2: ClassLoader): Class<any>;

        static loadProxyClass(arg0: String, arg1: String[], arg2: ClassLoader): Class<any>;

        static getClassLoader(arg0: String): ClassLoader;

        static getClassAnnotation(arg0: Class<any>): String;

        static getDefaultProviderInstance(): RMIClassLoaderSpi;

        static getSecurityContext(arg0: ClassLoader): Object;
    }

    export abstract class RMIClassLoaderSpi {
        constructor();

        abstract loadClass(arg0: String, arg1: String, arg2: ClassLoader): Class<any>;

        abstract loadProxyClass(arg0: String, arg1: String[], arg2: ClassLoader): Class<any>;

        abstract getClassLoader(arg0: String): ClassLoader;

        abstract getClassAnnotation(arg0: Class<any>): String;
    }

    export interface RMIClientSocketFactory {

        createSocket(arg0: String, arg1: number): Socket;
    }

    export interface RMIFailureHandler {

        failure(arg0: Exception): boolean;
    }

    export interface RMIServerSocketFactory {

        createServerSocket(arg0: number): ServerSocket;
    }

    export abstract class RMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory {
        constructor();

        abstract createSocket(arg0: String, arg1: number): Socket;

        abstract createServerSocket(arg0: number): ServerSocket;

        static setSocketFactory(arg0: RMISocketFactory): void;

        static getSocketFactory(): RMISocketFactory;

        static getDefaultSocketFactory(): RMISocketFactory;

        static setFailureHandler(arg0: RMIFailureHandler): void;

        static getFailureHandler(): RMIFailureHandler;
    }

    export interface RemoteCall {

        getOutputStream(): ObjectOutput;

        releaseOutputStream(): void;

        getInputStream(): ObjectInput;

        releaseInputStream(): void;

        getResultStream(arg0: boolean): ObjectOutput;

        executeCall(): void;

        done(): void;
    }

    export abstract class RemoteObject implements Remote, Serializable {

        getRef(): RemoteRef;

        static toStub(arg0: Remote): Remote;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export class RemoteObjectInvocationHandler extends RemoteObject implements InvocationHandler {
        constructor(arg0: RemoteRef);

        invoke(arg0: Object, arg1: Method, arg2: Object[]): Object;
    }

    export namespace RemoteRef {
        const serialVersionUID: number
        const packagePrefix: String
    }

    export interface RemoteRef extends Externalizable {
        serialVersionUID: number
        packagePrefix: String

        invoke(arg0: Remote, arg1: Method, arg2: Object[], arg3: number): Object;

        newCall(arg0: RemoteObject, arg1: Operation[], arg2: number, arg3: number): RemoteCall;

        invoke(arg0: RemoteCall): void;

        done(arg0: RemoteCall): void;

        getRefClass(arg0: ObjectOutput): String;

        remoteHashCode(): number;

        remoteEquals(arg0: RemoteRef): boolean;

        remoteToString(): String;
    }

    export abstract class RemoteServer extends RemoteObject {

        static getClientHost(): String;

        static setLog(arg0: OutputStream): void;

        static getLog(): PrintStream;
    }

    export abstract class RemoteStub extends RemoteObject {
    }

    export class ServerCloneException extends CloneNotSupportedException {
        detail: Exception
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);

        getMessage(): String;

        getCause(): Throwable;
    }

    export class ServerNotActiveException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export namespace ServerRef {
        const serialVersionUID: number
    }

    export interface ServerRef extends RemoteRef {
        serialVersionUID: number

        exportObject(arg0: Remote, arg1: Object): RemoteStub;

        getClientHost(): String;
    }

    export interface Skeleton {

        dispatch(arg0: Remote, arg1: RemoteCall, arg2: number, arg3: number): void;

        getOperations(): Operation[];
    }

    export class SkeletonMismatchException extends RemoteException {
        constructor(arg0: String);
    }

    export class SkeletonNotFoundException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class SocketSecurityException extends ExportException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class UID implements Serializable {
        constructor();
        constructor(arg0: number);

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;

        write(arg0: DataOutput): void;

        static read(arg0: DataInput): UID;
    }

    export class UnicastRemoteObject extends RemoteServer {

        clone(): Object;

        static exportObject(arg0: Remote): RemoteStub;

        static exportObject(arg0: Remote, arg1: number): Remote;

        static exportObject(arg0: Remote, arg1: number, arg2: RMIClientSocketFactory, arg3: RMIServerSocketFactory): Remote;

        static exportObject(arg0: Remote, arg1: number, arg2: ObjectInputFilter): Remote;

        static exportObject(arg0: Remote, arg1: number, arg2: RMIClientSocketFactory, arg3: RMIServerSocketFactory, arg4: ObjectInputFilter): Remote;

        static unexportObject(arg0: Remote, arg1: boolean): boolean;
    }

    export interface Unreferenced {

        unreferenced(): void;
    }

}
/// <reference path="java.rmi.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.rmi.server.d.ts" />
declare module '@java/java.rmi.registry' {
    import { Remote } from '@java/java.rmi'
    import { String } from '@java/java.lang'
    import { RMIServerSocketFactory, RMIClientSocketFactory } from '@java/java.rmi.server'
    export class LocateRegistry {

        static getRegistry(): Registry;

        static getRegistry(arg0: number): Registry;

        static getRegistry(arg0: String): Registry;

        static getRegistry(arg0: String, arg1: number): Registry;

        static getRegistry(arg0: String, arg1: number, arg2: RMIClientSocketFactory): Registry;

        static createRegistry(arg0: number): Registry;

        static createRegistry(arg0: number, arg1: RMIClientSocketFactory, arg2: RMIServerSocketFactory): Registry;
    }

    export namespace Registry {
        const REGISTRY_PORT: number
    }

    export interface Registry extends Remote {
        REGISTRY_PORT: number

        lookup(arg0: String): Remote;

        bind(arg0: String, arg1: Remote): void;

        unbind(arg0: String): void;

        rebind(arg0: String, arg1: Remote): void;

        list(): String[];
    }

    export interface RegistryHandler {

        registryStub(arg0: String, arg1: number): Registry;

        registryImpl(arg0: number): Registry;
    }

}
/// <reference path="java.rmi.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.rmi.server.d.ts" />
declare module '@java/java.rmi.dgc' {
    import { Remote } from '@java/java.rmi'
    import { Serializable } from '@java/java.io'
    import { ObjID } from '@java/java.rmi.server'
    export interface DGC extends Remote {

        dirty(arg0: ObjID[], arg1: number, arg2: Lease): Lease;

        clean(arg0: ObjID[], arg1: number, arg2: VMID, arg3: boolean): void;
    }

    export class Lease implements Serializable {
        constructor(arg0: VMID, arg1: number);

        getVMID(): VMID;

        getValue(): number;
    }

    export class VMID implements Serializable {
        constructor();

        static isUnique(): boolean;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.rmi' {
    import { Throwable, Error, SecurityException, String, SecurityManager, Exception } from '@java/java.lang'
    import { Serializable, IOException } from '@java/java.io'
    export class AccessException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class AlreadyBoundException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export class ConnectException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class ConnectIOException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class MarshalException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class MarshalledObject<T extends Object> extends Object implements Serializable {
        constructor(arg0: T);

        get(): T;

        hashCode(): number;

        equals(arg0: Object): boolean;
    }

    export class Naming {

        static lookup(arg0: String): Remote;

        static bind(arg0: String, arg1: Remote): void;

        static unbind(arg0: String): void;

        static rebind(arg0: String, arg1: Remote): void;

        static list(arg0: String): String[];
    }

    export class NoSuchObjectException extends RemoteException {
        constructor(arg0: String);
    }

    export class NotBoundException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export class RMISecurityException extends SecurityException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export class RMISecurityManager extends SecurityManager {
        constructor();
    }

    export interface Remote {
    }

    export class RemoteException extends IOException {
        detail: Throwable
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);

        getMessage(): String;

        getCause(): Throwable;
    }

    export class ServerError extends RemoteException {
        constructor(arg0: String, arg1: Error);
    }

    export class ServerException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class ServerRuntimeException extends RemoteException {
        constructor(arg0: String, arg1: Exception);
    }

    export class StubNotFoundException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class UnexpectedException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class UnknownHostException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

    export class UnmarshalException extends RemoteException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Exception);
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.file.attribute.d.ts" />
declare module '@java/java.nio.file.spi' {
    import { Map, List, Set } from '@java/java.util'
    import { Class, String } from '@java/java.lang'
    import { URI } from '@java/java.net'
    import { InputStream, OutputStream } from '@java/java.io'
    import { ExecutorService } from '@java/java.util.concurrent'
    import { AsynchronousFileChannel, SeekableByteChannel, FileChannel } from '@java/java.nio.channels'
    import { Path, OpenOption, FileStore, CopyOption, LinkOption, FileSystem, DirectoryStream, AccessMode } from '@java/java.nio.file'
    import { FileAttributeView, BasicFileAttributes, FileAttribute } from '@java/java.nio.file.attribute'
    export abstract class FileSystemProvider {

        static installedProviders(): List<FileSystemProvider>;

        abstract getScheme(): String;

        abstract newFileSystem(arg0: URI, arg1: Map<String, any>): FileSystem;

        abstract getFileSystem(arg0: URI): FileSystem;

        abstract getPath(arg0: URI): Path;

        newFileSystem(arg0: Path, arg1: Map<String, any>): FileSystem;

        newInputStream(arg0: Path, arg1: OpenOption[]): InputStream;

        newOutputStream(arg0: Path, arg1: OpenOption[]): OutputStream;

        newFileChannel(arg0: Path, arg1: Set<OpenOption>, arg2: FileAttribute<any>[]): FileChannel;

        newAsynchronousFileChannel(arg0: Path, arg1: Set<OpenOption>, arg2: ExecutorService, arg3: FileAttribute<any>[]): AsynchronousFileChannel;

        abstract newByteChannel(arg0: Path, arg1: Set<OpenOption>, arg2: FileAttribute<any>[]): SeekableByteChannel;

        abstract newDirectoryStream(arg0: Path, arg1: DirectoryStream.Filter<Path>): DirectoryStream<Path>;

        abstract createDirectory(arg0: Path, arg1: FileAttribute<any>[]): void;

        createSymbolicLink(arg0: Path, arg1: Path, arg2: FileAttribute<any>[]): void;

        createLink(arg0: Path, arg1: Path): void;

        abstract delete(arg0: Path): void;

        deleteIfExists(arg0: Path): boolean;

        readSymbolicLink(arg0: Path): Path;

        abstract copy(arg0: Path, arg1: Path, arg2: CopyOption[]): void;

        abstract move(arg0: Path, arg1: Path, arg2: CopyOption[]): void;

        abstract isSameFile(arg0: Path, arg1: Path): boolean;

        abstract isHidden(arg0: Path): boolean;

        abstract getFileStore(arg0: Path): FileStore;

        abstract checkAccess(arg0: Path, arg1: AccessMode[]): void;

        abstract getFileAttributeView<V extends FileAttributeView>(arg0: Path, arg1: Class<V>, arg2: LinkOption[]): V;

        abstract readAttributes<A extends BasicFileAttributes>(arg0: Path, arg1: Class<A>, arg2: LinkOption[]): A;

        abstract readAttributes(arg0: Path, arg1: String, arg2: LinkOption[]): Map<String, Object>;

        abstract setAttribute(arg0: Path, arg1: String, arg2: Object, arg3: LinkOption[]): void;
    }

    export abstract class FileTypeDetector {

        abstract probeContentType(arg0: Path): String;
    }

}
/// <reference path="java.nio.file.spi.d.ts" />
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.nio.file.attribute.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.nio.file' {
    import { FileSystemProvider } from '@java/java.nio.file.spi'
    import { BasicPermission } from '@java/java.security'
    import { Enum, IllegalStateException, Comparable, RuntimeException, Iterable, CharSequence, ClassLoader, Class, String, IllegalArgumentException, UnsupportedOperationException } from '@java/java.lang'
    import { Iterator, ConcurrentModificationException, List, Set, Map } from '@java/java.util'
    import { URI } from '@java/java.net'
    import { InputStream, Closeable, OutputStream, BufferedReader, IOException, File, BufferedWriter } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { TimeUnit } from '@java/java.util.concurrent'
    import { SeekableByteChannel } from '@java/java.nio.channels'
    import { BiPredicate } from '@java/java.util.function'
    import { UserPrincipalLookupService, FileTime, FileStoreAttributeView, PosixFilePermission, FileAttributeView, BasicFileAttributes, UserPrincipal, FileAttribute } from '@java/java.nio.file.attribute'
    import { Charset } from '@java/java.nio.charset'
    export class AccessDeniedException extends FileSystemException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String);
    }

    export class AccessMode extends Enum<AccessMode> {
        static READ: AccessMode
        static WRITE: AccessMode
        static EXECUTE: AccessMode

        static values(): AccessMode[];

        static valueOf(arg0: String): AccessMode;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class AtomicMoveNotSupportedException extends FileSystemException {
        constructor(arg0: String, arg1: String, arg2: String);
    }

    export class ClosedDirectoryStreamException extends IllegalStateException {
        constructor();
    }

    export class ClosedFileSystemException extends IllegalStateException {
        constructor();
    }

    export class ClosedWatchServiceException extends IllegalStateException {
        constructor();
    }

    export interface CopyOption {
    }

    export class DirectoryIteratorException extends ConcurrentModificationException {
        constructor(arg0: IOException);

        getCause(): IOException;
    }

    export class DirectoryNotEmptyException extends FileSystemException {
        constructor(arg0: String);
    }

    export interface DirectoryStream<T extends Object> extends Closeable, Iterable<T>, Object {

        iterator(): Iterator<T>;
    }
    export namespace DirectoryStream {
        export interface Filter<T extends Object> extends Object {

            accept(arg0: T): boolean;
        }

    }

    export class FileAlreadyExistsException extends FileSystemException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String);
    }

    export abstract class FileStore {

        abstract name(): String;

        abstract type(): String;

        abstract isReadOnly(): boolean;

        abstract getTotalSpace(): number;

        abstract getUsableSpace(): number;

        abstract getUnallocatedSpace(): number;

        getBlockSize(): number;

        abstract supportsFileAttributeView(arg0: Class<FileAttributeView>): boolean;

        abstract supportsFileAttributeView(arg0: String): boolean;

        abstract getFileStoreAttributeView<V extends FileStoreAttributeView>(arg0: Class<V>): V;

        abstract getAttribute(arg0: String): Object;
    }

    export abstract class FileSystem implements Closeable {

        abstract provider(): FileSystemProvider;

        abstract close(): void;

        abstract isOpen(): boolean;

        abstract isReadOnly(): boolean;

        abstract getSeparator(): String;

        abstract getRootDirectories(): Iterable<Path>;

        abstract getFileStores(): Iterable<FileStore>;

        abstract supportedFileAttributeViews(): Set<String>;

        abstract getPath(arg0: String, arg1: String[]): Path;

        abstract getPathMatcher(arg0: String): PathMatcher;

        abstract getUserPrincipalLookupService(): UserPrincipalLookupService;

        abstract newWatchService(): WatchService;
    }

    export class FileSystemAlreadyExistsException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class FileSystemException extends IOException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String);

        getFile(): String;

        getOtherFile(): String;

        getReason(): String;

        getMessage(): String;
    }

    export class FileSystemLoopException extends FileSystemException {
        constructor(arg0: String);
    }

    export class FileSystemNotFoundException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class FileSystems {

        static getDefault(): FileSystem;

        static getFileSystem(arg0: URI): FileSystem;

        static newFileSystem(arg0: URI, arg1: Map<String, any>): FileSystem;

        static newFileSystem(arg0: URI, arg1: Map<String, any>, arg2: ClassLoader): FileSystem;

        static newFileSystem(arg0: Path, arg1: ClassLoader): FileSystem;

        static newFileSystem(arg0: Path, arg1: Map<String, any>): FileSystem;

        static newFileSystem(arg0: Path): FileSystem;

        static newFileSystem(arg0: Path, arg1: Map<String, any>, arg2: ClassLoader): FileSystem;
    }

    export class FileVisitOption extends Enum<FileVisitOption> {
        static FOLLOW_LINKS: FileVisitOption

        static values(): FileVisitOption[];

        static valueOf(arg0: String): FileVisitOption;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class FileVisitResult extends Enum<FileVisitResult> {
        static CONTINUE: FileVisitResult
        static TERMINATE: FileVisitResult
        static SKIP_SUBTREE: FileVisitResult
        static SKIP_SIBLINGS: FileVisitResult

        static values(): FileVisitResult[];

        static valueOf(arg0: String): FileVisitResult;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface FileVisitor<T extends Object> extends Object {

        preVisitDirectory(arg0: T, arg1: BasicFileAttributes): FileVisitResult;

        visitFile(arg0: T, arg1: BasicFileAttributes): FileVisitResult;

        visitFileFailed(arg0: T, arg1: IOException): FileVisitResult;

        postVisitDirectory(arg0: T, arg1: IOException): FileVisitResult;
    }

    export class Files {

        static newInputStream(arg0: Path, arg1: OpenOption[]): InputStream;

        static newOutputStream(arg0: Path, arg1: OpenOption[]): OutputStream;

        static newByteChannel(arg0: Path, arg1: Set<OpenOption>, arg2: FileAttribute<any>[]): SeekableByteChannel;

        static newByteChannel(arg0: Path, arg1: OpenOption[]): SeekableByteChannel;

        static newDirectoryStream(arg0: Path): DirectoryStream<Path>;

        static newDirectoryStream(arg0: Path, arg1: String): DirectoryStream<Path>;

        static newDirectoryStream(arg0: Path, arg1: DirectoryStream.Filter<Path>): DirectoryStream<Path>;

        static createFile(arg0: Path, arg1: FileAttribute<any>[]): Path;

        static createDirectory(arg0: Path, arg1: FileAttribute<any>[]): Path;

        static createDirectories(arg0: Path, arg1: FileAttribute<any>[]): Path;

        static createTempFile(arg0: Path, arg1: String, arg2: String, arg3: FileAttribute<any>[]): Path;

        static createTempFile(arg0: String, arg1: String, arg2: FileAttribute<any>[]): Path;

        static createTempDirectory(arg0: Path, arg1: String, arg2: FileAttribute<any>[]): Path;

        static createTempDirectory(arg0: String, arg1: FileAttribute<any>[]): Path;

        static createSymbolicLink(arg0: Path, arg1: Path, arg2: FileAttribute<any>[]): Path;

        static createLink(arg0: Path, arg1: Path): Path;

        static delete(arg0: Path): void;

        static deleteIfExists(arg0: Path): boolean;

        static copy(arg0: Path, arg1: Path, arg2: CopyOption[]): Path;

        static move(arg0: Path, arg1: Path, arg2: CopyOption[]): Path;

        static readSymbolicLink(arg0: Path): Path;

        static getFileStore(arg0: Path): FileStore;

        static isSameFile(arg0: Path, arg1: Path): boolean;

        static mismatch(arg0: Path, arg1: Path): number;

        static isHidden(arg0: Path): boolean;

        static probeContentType(arg0: Path): String;

        static getFileAttributeView<V extends FileAttributeView>(arg0: Path, arg1: Class<V>, arg2: LinkOption[]): V;

        static readAttributes<A extends BasicFileAttributes>(arg0: Path, arg1: Class<A>, arg2: LinkOption[]): A;

        static setAttribute(arg0: Path, arg1: String, arg2: Object, arg3: LinkOption[]): Path;

        static getAttribute(arg0: Path, arg1: String, arg2: LinkOption[]): Object;

        static readAttributes(arg0: Path, arg1: String, arg2: LinkOption[]): Map<String, Object>;

        static getPosixFilePermissions(arg0: Path, arg1: LinkOption[]): Set<PosixFilePermission>;

        static setPosixFilePermissions(arg0: Path, arg1: Set<PosixFilePermission>): Path;

        static getOwner(arg0: Path, arg1: LinkOption[]): UserPrincipal;

        static setOwner(arg0: Path, arg1: UserPrincipal): Path;

        static isSymbolicLink(arg0: Path): boolean;

        static isDirectory(arg0: Path, arg1: LinkOption[]): boolean;

        static isRegularFile(arg0: Path, arg1: LinkOption[]): boolean;

        static getLastModifiedTime(arg0: Path, arg1: LinkOption[]): FileTime;

        static setLastModifiedTime(arg0: Path, arg1: FileTime): Path;

        static size(arg0: Path): number;

        static exists(arg0: Path, arg1: LinkOption[]): boolean;

        static notExists(arg0: Path, arg1: LinkOption[]): boolean;

        static isReadable(arg0: Path): boolean;

        static isWritable(arg0: Path): boolean;

        static isExecutable(arg0: Path): boolean;

        static walkFileTree(arg0: Path, arg1: Set<FileVisitOption>, arg2: number, arg3: FileVisitor<Path>): Path;

        static walkFileTree(arg0: Path, arg1: FileVisitor<Path>): Path;

        static newBufferedReader(arg0: Path, arg1: Charset): BufferedReader;

        static newBufferedReader(arg0: Path): BufferedReader;

        static newBufferedWriter(arg0: Path, arg1: Charset, arg2: OpenOption[]): BufferedWriter;

        static newBufferedWriter(arg0: Path, arg1: OpenOption[]): BufferedWriter;

        static copy(arg0: InputStream, arg1: Path, arg2: CopyOption[]): number;

        static copy(arg0: Path, arg1: OutputStream): number;

        static readAllBytes(arg0: Path): number[];

        static readString(arg0: Path): String;

        static readString(arg0: Path, arg1: Charset): String;

        static readAllLines(arg0: Path, arg1: Charset): List<String>;

        static readAllLines(arg0: Path): List<String>;

        static write(arg0: Path, arg1: number[], arg2: OpenOption[]): Path;

        static write(arg0: Path, arg1: Iterable<CharSequence>, arg2: Charset, arg3: OpenOption[]): Path;

        static write(arg0: Path, arg1: Iterable<CharSequence>, arg2: OpenOption[]): Path;

        static writeString(arg0: Path, arg1: CharSequence, arg2: OpenOption[]): Path;

        static writeString(arg0: Path, arg1: CharSequence, arg2: Charset, arg3: OpenOption[]): Path;

        static list(arg0: Path): Stream<Path>;

        static walk(arg0: Path, arg1: number, arg2: FileVisitOption[]): Stream<Path>;

        static walk(arg0: Path, arg1: FileVisitOption[]): Stream<Path>;

        static find(arg0: Path, arg1: number, arg2: BiPredicate<Path, BasicFileAttributes>, arg3: FileVisitOption[]): Stream<Path>;

        static lines(arg0: Path, arg1: Charset): Stream<String>;

        static lines(arg0: Path): Stream<String>;
    }

    export class InvalidPathException extends IllegalArgumentException {
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: String, arg1: String);

        getInput(): String;

        getReason(): String;

        getIndex(): number;

        getMessage(): String;
    }

    export class LinkOption extends Enum<LinkOption> implements OpenOption, CopyOption {
        static NOFOLLOW_LINKS: LinkOption

        static values(): LinkOption[];

        static valueOf(arg0: String): LinkOption;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class LinkPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export class NoSuchFileException extends FileSystemException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String);
    }

    export class NotDirectoryException extends FileSystemException {
        constructor(arg0: String);
    }

    export class NotLinkException extends FileSystemException {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String);
    }

    export interface OpenOption {
    }

    export namespace Path {
        function
/* default */ of(arg0: String, arg1: String[]): Path;
        function
/* default */ of(arg0: URI): Path;
    }

    export interface Path extends Comparable<Path>, Iterable<Path>, Watchable, Object {

        getFileSystem(): FileSystem;

        isAbsolute(): boolean;

        getRoot(): Path;

        getFileName(): Path;

        getParent(): Path;

        getNameCount(): number;

        getName(arg0: number): Path;

        subpath(arg0: number, arg1: number): Path;

        startsWith(arg0: Path): boolean;

/* default */ startsWith(arg0: String): boolean;

        endsWith(arg0: Path): boolean;

/* default */ endsWith(arg0: String): boolean;

        normalize(): Path;

        resolve(arg0: Path): Path;

/* default */ resolve(arg0: String): Path;

/* default */ resolveSibling(arg0: Path): Path;

/* default */ resolveSibling(arg0: String): Path;

        relativize(arg0: Path): Path;

        toUri(): URI;

        toAbsolutePath(): Path;

        toRealPath(arg0: LinkOption[]): Path;

/* default */ toFile(): File;

        register(arg0: WatchService, arg1: WatchEvent.Kind<any>[], arg2: WatchEvent.Modifier[]): WatchKey;

/* default */ register(arg0: WatchService, arg1: WatchEvent.Kind<any>[]): WatchKey;

/* default */ iterator(): Iterator<Path>;

        compareTo(arg0: Path): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export interface PathMatcher {

        matches(arg0: Path): boolean;
    }

    export class Paths {

        static get(arg0: String, arg1: String[]): Path;

        static get(arg0: URI): Path;
    }

    export class ProviderMismatchException extends IllegalArgumentException {
        constructor();
        constructor(arg0: String);
    }

    export class ProviderNotFoundException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class ReadOnlyFileSystemException extends UnsupportedOperationException {
        constructor();
    }

    export interface SecureDirectoryStream<T extends Object> extends DirectoryStream<T>, Object {

        newDirectoryStream(arg0: T, arg1: LinkOption[]): SecureDirectoryStream<T>;

        newByteChannel(arg0: T, arg1: Set<OpenOption>, arg2: FileAttribute<any>[]): SeekableByteChannel;

        deleteFile(arg0: T): void;

        deleteDirectory(arg0: T): void;

        move(arg0: T, arg1: SecureDirectoryStream<T>, arg2: T): void;

        getFileAttributeView<V extends FileAttributeView>(arg0: Class<V>): V;

        getFileAttributeView<V extends FileAttributeView>(arg0: T, arg1: Class<V>, arg2: LinkOption[]): V;
    }

    export class SimpleFileVisitor<T extends Object> extends Object implements FileVisitor<T> {

        preVisitDirectory(arg0: T, arg1: BasicFileAttributes): FileVisitResult;

        visitFile(arg0: T, arg1: BasicFileAttributes): FileVisitResult;

        visitFileFailed(arg0: T, arg1: IOException): FileVisitResult;

        postVisitDirectory(arg0: T, arg1: IOException): FileVisitResult;
    }

    export class StandardCopyOption extends Enum<StandardCopyOption> implements CopyOption {
        static REPLACE_EXISTING: StandardCopyOption
        static COPY_ATTRIBUTES: StandardCopyOption
        static ATOMIC_MOVE: StandardCopyOption

        static values(): StandardCopyOption[];

        static valueOf(arg0: String): StandardCopyOption;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class StandardOpenOption extends Enum<StandardOpenOption> implements OpenOption {
        static READ: StandardOpenOption
        static WRITE: StandardOpenOption
        static APPEND: StandardOpenOption
        static TRUNCATE_EXISTING: StandardOpenOption
        static CREATE: StandardOpenOption
        static CREATE_NEW: StandardOpenOption
        static DELETE_ON_CLOSE: StandardOpenOption
        static SPARSE: StandardOpenOption
        static SYNC: StandardOpenOption
        static DSYNC: StandardOpenOption

        static values(): StandardOpenOption[];

        static valueOf(arg0: String): StandardOpenOption;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class StandardWatchEventKinds {
        static OVERFLOW: WatchEvent.Kind<Object>
        static ENTRY_CREATE: WatchEvent.Kind<Path>
        static ENTRY_DELETE: WatchEvent.Kind<Path>
        static ENTRY_MODIFY: WatchEvent.Kind<Path>
    }

    export interface WatchEvent<T extends Object> extends Object {

        kind(): WatchEvent.Kind<T>;

        count(): number;

        context(): T;
    }
    export namespace WatchEvent {
        export interface Kind<T extends Object> extends Object {

            name(): String;

            type(): Class<T>;
        }

        export interface Modifier {

            name(): String;
        }

    }

    export interface WatchKey {

        isValid(): boolean;

        pollEvents(): List<WatchEvent<any>>;

        reset(): boolean;

        cancel(): void;

        watchable(): Watchable;
    }

    export interface WatchService extends Closeable {

        close(): void;

        poll(): WatchKey;

        poll(arg0: number, arg1: TimeUnit): WatchKey;

        take(): WatchKey;
    }

    export interface Watchable {

        register(arg0: WatchService, arg1: WatchEvent.Kind<any>[], arg2: WatchEvent.Modifier[]): WatchKey;

        register(arg0: WatchService, arg1: WatchEvent.Kind<any>[]): WatchKey;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.nio.d.ts" />
declare module '@java/java.nio.file.attribute' {
    import { Principal } from '@java/java.security'
    import { List, Set } from '@java/java.util'
    import { Enum, Class, Comparable, String } from '@java/java.lang'
    import { Instant } from '@java/java.time'
    import { IOException } from '@java/java.io'
    import { TimeUnit } from '@java/java.util.concurrent'
    import { ByteBuffer } from '@java/java.nio'
    export class AclEntry {

        static newBuilder(): AclEntry.Builder;

        static newBuilder(arg0: AclEntry): AclEntry.Builder;

        type(): AclEntryType;

        principal(): UserPrincipal;

        permissions(): Set<AclEntryPermission>;

        flags(): Set<AclEntryFlag>;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }
    export namespace AclEntry {
        export class Builder {

            build(): AclEntry;

            setType(arg0: AclEntryType): AclEntry.Builder;

            setPrincipal(arg0: UserPrincipal): AclEntry.Builder;

            setPermissions(arg0: Set<AclEntryPermission>): AclEntry.Builder;

            setPermissions(arg0: AclEntryPermission[]): AclEntry.Builder;

            setFlags(arg0: Set<AclEntryFlag>): AclEntry.Builder;

            setFlags(arg0: AclEntryFlag[]): AclEntry.Builder;
        }

    }

    export class AclEntryFlag extends Enum<AclEntryFlag> {
        static FILE_INHERIT: AclEntryFlag
        static DIRECTORY_INHERIT: AclEntryFlag
        static NO_PROPAGATE_INHERIT: AclEntryFlag
        static INHERIT_ONLY: AclEntryFlag

        static values(): AclEntryFlag[];

        static valueOf(arg0: String): AclEntryFlag;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class AclEntryPermission extends Enum<AclEntryPermission> {
        static READ_DATA: AclEntryPermission
        static WRITE_DATA: AclEntryPermission
        static APPEND_DATA: AclEntryPermission
        static READ_NAMED_ATTRS: AclEntryPermission
        static WRITE_NAMED_ATTRS: AclEntryPermission
        static EXECUTE: AclEntryPermission
        static DELETE_CHILD: AclEntryPermission
        static READ_ATTRIBUTES: AclEntryPermission
        static WRITE_ATTRIBUTES: AclEntryPermission
        static DELETE: AclEntryPermission
        static READ_ACL: AclEntryPermission
        static WRITE_ACL: AclEntryPermission
        static WRITE_OWNER: AclEntryPermission
        static SYNCHRONIZE: AclEntryPermission
        static LIST_DIRECTORY: AclEntryPermission
        static ADD_FILE: AclEntryPermission
        static ADD_SUBDIRECTORY: AclEntryPermission

        static values(): AclEntryPermission[];

        static valueOf(arg0: String): AclEntryPermission;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class AclEntryType extends Enum<AclEntryType> {
        static ALLOW: AclEntryType
        static DENY: AclEntryType
        static AUDIT: AclEntryType
        static ALARM: AclEntryType

        static values(): AclEntryType[];

        static valueOf(arg0: String): AclEntryType;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export interface AclFileAttributeView extends FileOwnerAttributeView {

        name(): String;

        getAcl(): List<AclEntry>;

        setAcl(arg0: List<AclEntry>): void;
    }

    export interface AttributeView {

        name(): String;
    }

    export interface BasicFileAttributeView extends FileAttributeView {

        name(): String;

        readAttributes(): BasicFileAttributes;

        setTimes(arg0: FileTime, arg1: FileTime, arg2: FileTime): void;
    }

    export interface BasicFileAttributes {

        lastModifiedTime(): FileTime;

        lastAccessTime(): FileTime;

        creationTime(): FileTime;

        isRegularFile(): boolean;

        isDirectory(): boolean;

        isSymbolicLink(): boolean;

        isOther(): boolean;

        size(): number;

        fileKey(): Object;
    }

    export interface DosFileAttributeView extends BasicFileAttributeView {

        name(): String;

        readAttributes(): DosFileAttributes;

        setReadOnly(arg0: boolean): void;

        setHidden(arg0: boolean): void;

        setSystem(arg0: boolean): void;

        setArchive(arg0: boolean): void;
    }

    export interface DosFileAttributes extends BasicFileAttributes {

        isReadOnly(): boolean;

        isHidden(): boolean;

        isArchive(): boolean;

        isSystem(): boolean;
    }

    export interface FileAttribute<T extends Object> extends Object {

        name(): String;

        value(): T;
    }

    export interface FileAttributeView extends AttributeView {
    }

    export interface FileOwnerAttributeView extends FileAttributeView {

        name(): String;

        getOwner(): UserPrincipal;

        setOwner(arg0: UserPrincipal): void;
    }

    export interface FileStoreAttributeView extends AttributeView {
    }

    export class FileTime extends Object implements Comparable<FileTime> {

        static from(arg0: number, arg1: TimeUnit): FileTime;

        static fromMillis(arg0: number): FileTime;

        static from(arg0: Instant): FileTime;

        to(arg0: TimeUnit): number;

        toMillis(): number;

        toInstant(): Instant;

        equals(arg0: Object): boolean;

        hashCode(): number;

        compareTo(arg0: FileTime): number;
        toString(): string;
    }

    export interface GroupPrincipal extends UserPrincipal {
    }

    export interface PosixFileAttributeView extends BasicFileAttributeView, FileOwnerAttributeView {

        name(): String;

        readAttributes(): PosixFileAttributes;

        setPermissions(arg0: Set<PosixFilePermission>): void;

        setGroup(arg0: GroupPrincipal): void;
    }

    export interface PosixFileAttributes extends BasicFileAttributes {

        owner(): UserPrincipal;

        group(): GroupPrincipal;

        permissions(): Set<PosixFilePermission>;
    }

    export class PosixFilePermission extends Enum<PosixFilePermission> {
        static OWNER_READ: PosixFilePermission
        static OWNER_WRITE: PosixFilePermission
        static OWNER_EXECUTE: PosixFilePermission
        static GROUP_READ: PosixFilePermission
        static GROUP_WRITE: PosixFilePermission
        static GROUP_EXECUTE: PosixFilePermission
        static OTHERS_READ: PosixFilePermission
        static OTHERS_WRITE: PosixFilePermission
        static OTHERS_EXECUTE: PosixFilePermission

        static values(): PosixFilePermission[];

        static valueOf(arg0: String): PosixFilePermission;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class PosixFilePermissions {

        static toString(arg0: Set<PosixFilePermission>): String;

        static fromString(arg0: String): Set<PosixFilePermission>;

        static asFileAttribute(arg0: Set<PosixFilePermission>): FileAttribute<Set<PosixFilePermission>>;
    }

    export interface UserDefinedFileAttributeView extends FileAttributeView {

        name(): String;

        list(): List<String>;

        size(arg0: String): number;

        read(arg0: String, arg1: ByteBuffer): number;

        write(arg0: String, arg1: ByteBuffer): number;

        delete(arg0: String): void;
    }

    export interface UserPrincipal extends Principal {
    }

    export abstract class UserPrincipalLookupService {

        abstract lookupPrincipalByName(arg0: String): UserPrincipal;

        abstract lookupPrincipalByGroupName(arg0: String): GroupPrincipal;
    }

    export class UserPrincipalNotFoundException extends IOException {
        constructor(arg0: String);

        getName(): String;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.stream.d.ts" />
declare module '@java/java.nio' {
    import { IllegalStateException, Comparable, RuntimeException, Appendable, CharSequence, Readable, String, UnsupportedOperationException } from '@java/java.lang'
    import { IntStream } from '@java/java.util.stream'
    export abstract class Buffer {

        capacity(): number;

        position(): number;

        position(arg0: number): Buffer;

        limit(): number;

        limit(arg0: number): Buffer;

        mark(): Buffer;

        reset(): Buffer;

        clear(): Buffer;

        flip(): Buffer;

        rewind(): Buffer;

        remaining(): number;

        hasRemaining(): boolean;

        abstract isReadOnly(): boolean;

        abstract hasArray(): boolean;

        abstract array(): Object;

        abstract arrayOffset(): number;

        abstract isDirect(): boolean;

        abstract slice(): Buffer;

        abstract slice(arg0: number, arg1: number): Buffer;

        abstract duplicate(): Buffer;
    }

    export class BufferOverflowException extends RuntimeException {
        constructor();
    }

    export class BufferUnderflowException extends RuntimeException {
        constructor();
    }

    export abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {

        static allocateDirect(arg0: number): ByteBuffer;

        static allocate(arg0: number): ByteBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): ByteBuffer;

        static wrap(arg0: number[]): ByteBuffer;

        abstract slice(): ByteBuffer;

        abstract slice(arg0: number, arg1: number): ByteBuffer;

        abstract duplicate(): ByteBuffer;

        abstract asReadOnlyBuffer(): ByteBuffer;

        abstract get(): number;

        abstract put(arg0: number): ByteBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): ByteBuffer;

        get(arg0: number[], arg1: number, arg2: number): ByteBuffer;

        get(arg0: number[]): ByteBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): ByteBuffer;

        get(arg0: number, arg1: number[]): ByteBuffer;

        put(arg0: ByteBuffer): ByteBuffer;

        put(arg0: number, arg1: ByteBuffer, arg2: number, arg3: number): ByteBuffer;

        put(arg0: number[], arg1: number, arg2: number): ByteBuffer;

        put(arg0: number[]): ByteBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): ByteBuffer;

        put(arg0: number, arg1: number[]): ByteBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): ByteBuffer;

        limit(arg0: number): ByteBuffer;

        mark(): ByteBuffer;

        reset(): ByteBuffer;

        clear(): ByteBuffer;

        flip(): ByteBuffer;

        rewind(): ByteBuffer;

        abstract compact(): ByteBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: ByteBuffer): number;

        mismatch(arg0: ByteBuffer): number;

        order(): ByteOrder;

        order(arg0: ByteOrder): ByteBuffer;

        alignmentOffset(arg0: number, arg1: number): number;

        alignedSlice(arg0: number): ByteBuffer;

        abstract getChar(): String;

        abstract putChar(arg0: String): ByteBuffer;

        abstract getChar(arg0: number): String;

        abstract putChar(arg0: number, arg1: String): ByteBuffer;

        abstract asCharBuffer(): CharBuffer;

        abstract getShort(): number;

        abstract putShort(arg0: number): ByteBuffer;

        abstract getShort(arg0: number): number;

        abstract putShort(arg0: number, arg1: number): ByteBuffer;

        abstract asShortBuffer(): ShortBuffer;

        abstract getInt(): number;

        abstract putInt(arg0: number): ByteBuffer;

        abstract getInt(arg0: number): number;

        abstract putInt(arg0: number, arg1: number): ByteBuffer;

        abstract asIntBuffer(): IntBuffer;

        abstract getLong(): number;

        abstract putLong(arg0: number): ByteBuffer;

        abstract getLong(arg0: number): number;

        abstract putLong(arg0: number, arg1: number): ByteBuffer;

        abstract asLongBuffer(): LongBuffer;

        abstract getFloat(): number;

        abstract putFloat(arg0: number): ByteBuffer;

        abstract getFloat(arg0: number): number;

        abstract putFloat(arg0: number, arg1: number): ByteBuffer;

        abstract asFloatBuffer(): FloatBuffer;

        abstract getDouble(): number;

        abstract putDouble(arg0: number): ByteBuffer;

        abstract getDouble(arg0: number): number;

        abstract putDouble(arg0: number, arg1: number): ByteBuffer;

        abstract asDoubleBuffer(): DoubleBuffer;
    }

    export class ByteOrder {
        static BIG_ENDIAN: ByteOrder
        static LITTLE_ENDIAN: ByteOrder

        static nativeOrder(): ByteOrder;
        toString(): string;
    }

    export interface CharBuffer extends Comparable<CharBuffer>, Appendable, CharSequence, Readable { }
    export abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>, Appendable, CharSequence, Readable {

        static allocate(arg0: number): CharBuffer;

        static wrap(arg0: String[], arg1: number, arg2: number): CharBuffer;

        static wrap(arg0: String[]): CharBuffer;

        read(arg0: CharBuffer): number;

        static wrap(arg0: CharSequence, arg1: number, arg2: number): CharBuffer;

        static wrap(arg0: CharSequence): CharBuffer;

        abstract slice(): CharBuffer;

        abstract slice(arg0: number, arg1: number): CharBuffer;

        abstract duplicate(): CharBuffer;

        abstract asReadOnlyBuffer(): CharBuffer;

        abstract get(): String;

        abstract put(arg0: String): CharBuffer;

        abstract get(arg0: number): String;

        abstract put(arg0: number, arg1: String): CharBuffer;

        get(arg0: String[], arg1: number, arg2: number): CharBuffer;

        get(arg0: String[]): CharBuffer;

        get(arg0: number, arg1: String[], arg2: number, arg3: number): CharBuffer;

        get(arg0: number, arg1: String[]): CharBuffer;

        put(arg0: CharBuffer): CharBuffer;

        put(arg0: number, arg1: CharBuffer, arg2: number, arg3: number): CharBuffer;

        put(arg0: String[], arg1: number, arg2: number): CharBuffer;

        put(arg0: String[]): CharBuffer;

        put(arg0: number, arg1: String[], arg2: number, arg3: number): CharBuffer;

        put(arg0: number, arg1: String[]): CharBuffer;

        put(arg0: String, arg1: number, arg2: number): CharBuffer;

        put(arg0: String): CharBuffer;

        hasArray(): boolean;

        array(): String[];

        arrayOffset(): number;

        position(arg0: number): CharBuffer;

        limit(arg0: number): CharBuffer;

        mark(): CharBuffer;

        reset(): CharBuffer;

        clear(): CharBuffer;

        flip(): CharBuffer;

        rewind(): CharBuffer;

        abstract compact(): CharBuffer;

        abstract isDirect(): boolean;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: CharBuffer): number;

        mismatch(arg0: CharBuffer): number;
        toString(): string;

        length(): number;

        isEmpty(): boolean;

        charAt(arg0: number): String;

        abstract subSequence(arg0: number, arg1: number): CharBuffer;

        append(arg0: CharSequence): CharBuffer;

        append(arg0: CharSequence, arg1: number, arg2: number): CharBuffer;

        append(arg0: String): CharBuffer;

        abstract order(): ByteOrder;

        chars(): IntStream;
    }

    export abstract class DoubleBuffer extends Buffer implements Comparable<DoubleBuffer> {

        static allocate(arg0: number): DoubleBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): DoubleBuffer;

        static wrap(arg0: number[]): DoubleBuffer;

        abstract slice(): DoubleBuffer;

        abstract slice(arg0: number, arg1: number): DoubleBuffer;

        abstract duplicate(): DoubleBuffer;

        abstract asReadOnlyBuffer(): DoubleBuffer;

        abstract get(): number;

        abstract put(arg0: number): DoubleBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): DoubleBuffer;

        get(arg0: number[], arg1: number, arg2: number): DoubleBuffer;

        get(arg0: number[]): DoubleBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): DoubleBuffer;

        get(arg0: number, arg1: number[]): DoubleBuffer;

        put(arg0: DoubleBuffer): DoubleBuffer;

        put(arg0: number, arg1: DoubleBuffer, arg2: number, arg3: number): DoubleBuffer;

        put(arg0: number[], arg1: number, arg2: number): DoubleBuffer;

        put(arg0: number[]): DoubleBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): DoubleBuffer;

        put(arg0: number, arg1: number[]): DoubleBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): DoubleBuffer;

        limit(arg0: number): DoubleBuffer;

        mark(): DoubleBuffer;

        reset(): DoubleBuffer;

        clear(): DoubleBuffer;

        flip(): DoubleBuffer;

        rewind(): DoubleBuffer;

        abstract compact(): DoubleBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: DoubleBuffer): number;

        mismatch(arg0: DoubleBuffer): number;

        abstract order(): ByteOrder;
    }

    export abstract class FloatBuffer extends Buffer implements Comparable<FloatBuffer> {

        static allocate(arg0: number): FloatBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): FloatBuffer;

        static wrap(arg0: number[]): FloatBuffer;

        abstract slice(): FloatBuffer;

        abstract slice(arg0: number, arg1: number): FloatBuffer;

        abstract duplicate(): FloatBuffer;

        abstract asReadOnlyBuffer(): FloatBuffer;

        abstract get(): number;

        abstract put(arg0: number): FloatBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): FloatBuffer;

        get(arg0: number[], arg1: number, arg2: number): FloatBuffer;

        get(arg0: number[]): FloatBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): FloatBuffer;

        get(arg0: number, arg1: number[]): FloatBuffer;

        put(arg0: FloatBuffer): FloatBuffer;

        put(arg0: number, arg1: FloatBuffer, arg2: number, arg3: number): FloatBuffer;

        put(arg0: number[], arg1: number, arg2: number): FloatBuffer;

        put(arg0: number[]): FloatBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): FloatBuffer;

        put(arg0: number, arg1: number[]): FloatBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): FloatBuffer;

        limit(arg0: number): FloatBuffer;

        mark(): FloatBuffer;

        reset(): FloatBuffer;

        clear(): FloatBuffer;

        flip(): FloatBuffer;

        rewind(): FloatBuffer;

        abstract compact(): FloatBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: FloatBuffer): number;

        mismatch(arg0: FloatBuffer): number;

        abstract order(): ByteOrder;
    }

    export abstract class IntBuffer extends Buffer implements Comparable<IntBuffer> {

        static allocate(arg0: number): IntBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): IntBuffer;

        static wrap(arg0: number[]): IntBuffer;

        abstract slice(): IntBuffer;

        abstract slice(arg0: number, arg1: number): IntBuffer;

        abstract duplicate(): IntBuffer;

        abstract asReadOnlyBuffer(): IntBuffer;

        abstract get(): number;

        abstract put(arg0: number): IntBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): IntBuffer;

        get(arg0: number[], arg1: number, arg2: number): IntBuffer;

        get(arg0: number[]): IntBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): IntBuffer;

        get(arg0: number, arg1: number[]): IntBuffer;

        put(arg0: IntBuffer): IntBuffer;

        put(arg0: number, arg1: IntBuffer, arg2: number, arg3: number): IntBuffer;

        put(arg0: number[], arg1: number, arg2: number): IntBuffer;

        put(arg0: number[]): IntBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): IntBuffer;

        put(arg0: number, arg1: number[]): IntBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): IntBuffer;

        limit(arg0: number): IntBuffer;

        mark(): IntBuffer;

        reset(): IntBuffer;

        clear(): IntBuffer;

        flip(): IntBuffer;

        rewind(): IntBuffer;

        abstract compact(): IntBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: IntBuffer): number;

        mismatch(arg0: IntBuffer): number;

        abstract order(): ByteOrder;
    }

    export class InvalidMarkException extends IllegalStateException {
        constructor();
    }

    export abstract class LongBuffer extends Buffer implements Comparable<LongBuffer> {

        static allocate(arg0: number): LongBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): LongBuffer;

        static wrap(arg0: number[]): LongBuffer;

        abstract slice(): LongBuffer;

        abstract slice(arg0: number, arg1: number): LongBuffer;

        abstract duplicate(): LongBuffer;

        abstract asReadOnlyBuffer(): LongBuffer;

        abstract get(): number;

        abstract put(arg0: number): LongBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): LongBuffer;

        get(arg0: number[], arg1: number, arg2: number): LongBuffer;

        get(arg0: number[]): LongBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): LongBuffer;

        get(arg0: number, arg1: number[]): LongBuffer;

        put(arg0: LongBuffer): LongBuffer;

        put(arg0: number, arg1: LongBuffer, arg2: number, arg3: number): LongBuffer;

        put(arg0: number[], arg1: number, arg2: number): LongBuffer;

        put(arg0: number[]): LongBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): LongBuffer;

        put(arg0: number, arg1: number[]): LongBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): LongBuffer;

        limit(arg0: number): LongBuffer;

        mark(): LongBuffer;

        reset(): LongBuffer;

        clear(): LongBuffer;

        flip(): LongBuffer;

        rewind(): LongBuffer;

        abstract compact(): LongBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: LongBuffer): number;

        mismatch(arg0: LongBuffer): number;

        abstract order(): ByteOrder;
    }

    export abstract class MappedByteBuffer extends ByteBuffer {

        isLoaded(): boolean;

        load(): MappedByteBuffer;

        force(): MappedByteBuffer;

        force(arg0: number, arg1: number): MappedByteBuffer;

        position(arg0: number): MappedByteBuffer;

        limit(arg0: number): MappedByteBuffer;

        mark(): MappedByteBuffer;

        reset(): MappedByteBuffer;

        clear(): MappedByteBuffer;

        flip(): MappedByteBuffer;

        rewind(): MappedByteBuffer;

        abstract slice(): MappedByteBuffer;

        abstract slice(arg0: number, arg1: number): MappedByteBuffer;

        abstract duplicate(): MappedByteBuffer;

        abstract compact(): MappedByteBuffer;
    }

    export class ReadOnlyBufferException extends UnsupportedOperationException {
        constructor();
    }

    export abstract class ShortBuffer extends Buffer implements Comparable<ShortBuffer> {

        static allocate(arg0: number): ShortBuffer;

        static wrap(arg0: number[], arg1: number, arg2: number): ShortBuffer;

        static wrap(arg0: number[]): ShortBuffer;

        abstract slice(): ShortBuffer;

        abstract slice(arg0: number, arg1: number): ShortBuffer;

        abstract duplicate(): ShortBuffer;

        abstract asReadOnlyBuffer(): ShortBuffer;

        abstract get(): number;

        abstract put(arg0: number): ShortBuffer;

        abstract get(arg0: number): number;

        abstract put(arg0: number, arg1: number): ShortBuffer;

        get(arg0: number[], arg1: number, arg2: number): ShortBuffer;

        get(arg0: number[]): ShortBuffer;

        get(arg0: number, arg1: number[], arg2: number, arg3: number): ShortBuffer;

        get(arg0: number, arg1: number[]): ShortBuffer;

        put(arg0: ShortBuffer): ShortBuffer;

        put(arg0: number, arg1: ShortBuffer, arg2: number, arg3: number): ShortBuffer;

        put(arg0: number[], arg1: number, arg2: number): ShortBuffer;

        put(arg0: number[]): ShortBuffer;

        put(arg0: number, arg1: number[], arg2: number, arg3: number): ShortBuffer;

        put(arg0: number, arg1: number[]): ShortBuffer;

        hasArray(): boolean;

        array(): number[];

        arrayOffset(): number;

        position(arg0: number): ShortBuffer;

        limit(arg0: number): ShortBuffer;

        mark(): ShortBuffer;

        reset(): ShortBuffer;

        clear(): ShortBuffer;

        flip(): ShortBuffer;

        rewind(): ShortBuffer;

        abstract compact(): ShortBuffer;

        abstract isDirect(): boolean;
        toString(): string;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: ShortBuffer): number;

        mismatch(arg0: ShortBuffer): number;

        abstract order(): ByteOrder;
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.nio.charset.spi' {
    import { Iterator } from '@java/java.util'
    import { String } from '@java/java.lang'
    import { Charset } from '@java/java.nio.charset'
    export abstract class CharsetProvider {

        abstract charsets(): Iterator<Charset>;

        abstract charsetForName(arg0: String): Charset;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.nio.d.ts" />
declare module '@java/java.nio.charset' {
    import { CharSequence, Error, Comparable, String, Exception, IllegalArgumentException } from '@java/java.lang'
    import { Locale, SortedMap, Set } from '@java/java.util'
    import { IOException } from '@java/java.io'
    import { CharBuffer, ByteBuffer } from '@java/java.nio'
    export class CharacterCodingException extends IOException {
        constructor();
    }

    export abstract class Charset extends Object implements Comparable<Charset> {

        static isSupported(arg0: String): boolean;

        static forName(arg0: String): Charset;

        static availableCharsets(): SortedMap<String, Charset>;

        static defaultCharset(): Charset;

        name(): String;

        aliases(): Set<String>;

        displayName(): String;

        isRegistered(): boolean;

        displayName(arg0: Locale): String;

        abstract contains(arg0: Charset): boolean;

        abstract newDecoder(): CharsetDecoder;

        abstract newEncoder(): CharsetEncoder;

        canEncode(): boolean;

        decode(arg0: ByteBuffer): CharBuffer;

        encode(arg0: CharBuffer): ByteBuffer;

        encode(arg0: String): ByteBuffer;

        compareTo(arg0: Charset): number;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export abstract class CharsetDecoder {

        charset(): Charset;

        replacement(): String;

        replaceWith(arg0: String): CharsetDecoder;

        malformedInputAction(): CodingErrorAction;

        onMalformedInput(arg0: CodingErrorAction): CharsetDecoder;

        unmappableCharacterAction(): CodingErrorAction;

        onUnmappableCharacter(arg0: CodingErrorAction): CharsetDecoder;

        averageCharsPerByte(): number;

        maxCharsPerByte(): number;

        decode(arg0: ByteBuffer, arg1: CharBuffer, arg2: boolean): CoderResult;

        flush(arg0: CharBuffer): CoderResult;

        reset(): CharsetDecoder;

        decode(arg0: ByteBuffer): CharBuffer;

        isAutoDetecting(): boolean;

        isCharsetDetected(): boolean;

        detectedCharset(): Charset;
    }

    export abstract class CharsetEncoder {

        charset(): Charset;

        replacement(): number[];

        replaceWith(arg0: number[]): CharsetEncoder;

        isLegalReplacement(arg0: number[]): boolean;

        malformedInputAction(): CodingErrorAction;

        onMalformedInput(arg0: CodingErrorAction): CharsetEncoder;

        unmappableCharacterAction(): CodingErrorAction;

        onUnmappableCharacter(arg0: CodingErrorAction): CharsetEncoder;

        averageBytesPerChar(): number;

        maxBytesPerChar(): number;

        encode(arg0: CharBuffer, arg1: ByteBuffer, arg2: boolean): CoderResult;

        flush(arg0: ByteBuffer): CoderResult;

        reset(): CharsetEncoder;

        encode(arg0: CharBuffer): ByteBuffer;

        canEncode(arg0: String): boolean;

        canEncode(arg0: CharSequence): boolean;
    }

    export class CoderMalfunctionError extends Error {
        constructor(arg0: Exception);
    }

    export class CoderResult {
        static UNDERFLOW: CoderResult
        static OVERFLOW: CoderResult
        toString(): string;

        isUnderflow(): boolean;

        isOverflow(): boolean;

        isError(): boolean;

        isMalformed(): boolean;

        isUnmappable(): boolean;

        length(): number;

        static malformedForLength(arg0: number): CoderResult;

        static unmappableForLength(arg0: number): CoderResult;

        throwException(): void;
    }

    export class CodingErrorAction {
        static IGNORE: CodingErrorAction
        static REPLACE: CodingErrorAction
        static REPORT: CodingErrorAction
        toString(): string;
    }

    export class IllegalCharsetNameException extends IllegalArgumentException {
        constructor(arg0: String);

        getCharsetName(): String;
    }

    export class MalformedInputException extends CharacterCodingException {
        constructor(arg0: number);

        getInputLength(): number;

        getMessage(): String;
    }

    export class StandardCharsets {
        static US_ASCII: Charset
        static ISO_8859_1: Charset
        static UTF_8: Charset
        static UTF_16BE: Charset
        static UTF_16LE: Charset
        static UTF_16: Charset
    }

    export class UnmappableCharacterException extends CharacterCodingException {
        constructor(arg0: number);

        getInputLength(): number;

        getMessage(): String;
    }

    export class UnsupportedCharsetException extends IllegalArgumentException {
        constructor(arg0: String);

        getCharsetName(): String;
    }

}
/// <reference path="java.net.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
declare module '@java/java.nio.channels.spi' {
    import { ProtocolFamily } from '@java/java.net'
    import { ExecutorService, ThreadFactory } from '@java/java.util.concurrent'
    import { SelectionKey, SocketChannel, SelectableChannel, AsynchronousSocketChannel, InterruptibleChannel, Channel, AsynchronousChannelGroup, AsynchronousServerSocketChannel, DatagramChannel, Pipe, ServerSocketChannel, Selector } from '@java/java.nio.channels'
    export abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {

        close(): void;

        isOpen(): boolean;
    }

    export abstract class AbstractSelectableChannel extends SelectableChannel {

        provider(): SelectorProvider;

        isRegistered(): boolean;

        keyFor(arg0: Selector): SelectionKey;

        register(arg0: Selector, arg1: number, arg2: Object): SelectionKey;

        isBlocking(): boolean;

        blockingLock(): Object;

        configureBlocking(arg0: boolean): SelectableChannel;
    }

    export abstract class AbstractSelectionKey extends SelectionKey {

        isValid(): boolean;

        cancel(): void;
    }

    export abstract class AbstractSelector extends Selector {

        close(): void;

        isOpen(): boolean;

        provider(): SelectorProvider;
    }

    export abstract class AsynchronousChannelProvider {

        static provider(): AsynchronousChannelProvider;

        abstract openAsynchronousChannelGroup(arg0: number, arg1: ThreadFactory): AsynchronousChannelGroup;

        abstract openAsynchronousChannelGroup(arg0: ExecutorService, arg1: number): AsynchronousChannelGroup;

        abstract openAsynchronousServerSocketChannel(arg0: AsynchronousChannelGroup): AsynchronousServerSocketChannel;

        abstract openAsynchronousSocketChannel(arg0: AsynchronousChannelGroup): AsynchronousSocketChannel;
    }

    export abstract class SelectorProvider {

        static provider(): SelectorProvider;

        abstract openDatagramChannel(): DatagramChannel;

        abstract openDatagramChannel(arg0: ProtocolFamily): DatagramChannel;

        abstract openPipe(): Pipe;

        abstract openSelector(): AbstractSelector;

        abstract openServerSocketChannel(): ServerSocketChannel;

        abstract openSocketChannel(): SocketChannel;

        inheritedChannel(): Channel;

        openSocketChannel(arg0: ProtocolFamily): SocketChannel;

        openServerSocketChannel(arg0: ProtocolFamily): ServerSocketChannel;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.nio.channels.spi.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.file.attribute.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.nio.channels' {
    import { Integer, IllegalStateException, AutoCloseable, Long, Throwable, String, Void, IllegalArgumentException } from '@java/java.lang'
    import { Set } from '@java/java.util'
    import { ProtocolFamily, SocketOption, DatagramSocket, NetworkInterface, ServerSocket, SocketAddress, InetAddress, Socket } from '@java/java.net'
    import { Reader, IOException, InputStream, Writer, Closeable, OutputStream } from '@java/java.io'
    import { ExecutorService, Future, ThreadFactory, TimeUnit } from '@java/java.util.concurrent'
    import { AsynchronousChannelProvider, AbstractInterruptibleChannel, SelectorProvider, AbstractSelectableChannel } from '@java/java.nio.channels.spi'
    import { MappedByteBuffer, ByteBuffer } from '@java/java.nio'
    import { Consumer } from '@java/java.util.function'
    import { Path, OpenOption } from '@java/java.nio.file'
    import { FileAttribute } from '@java/java.nio.file.attribute'
    import { Charset, CharsetEncoder, CharsetDecoder } from '@java/java.nio.charset'
    export class AcceptPendingException extends IllegalStateException {
        constructor();
    }

    export class AlreadyBoundException extends IllegalStateException {
        constructor();
    }

    export class AlreadyConnectedException extends IllegalStateException {
        constructor();
    }

    export interface AsynchronousByteChannel extends AsynchronousChannel {

        read<A extends Object>(arg0: ByteBuffer, arg1: A, arg2: CompletionHandler<Number, A>): void;

        read(arg0: ByteBuffer): Future<Number>;

        write<A extends Object>(arg0: ByteBuffer, arg1: A, arg2: CompletionHandler<Number, A>): void;

        write(arg0: ByteBuffer): Future<Number>;
    }

    export interface AsynchronousChannel extends Channel {

        close(): void;
    }

    export abstract class AsynchronousChannelGroup {

        provider(): AsynchronousChannelProvider;

        static withFixedThreadPool(arg0: number, arg1: ThreadFactory): AsynchronousChannelGroup;

        static withCachedThreadPool(arg0: ExecutorService, arg1: number): AsynchronousChannelGroup;

        static withThreadPool(arg0: ExecutorService): AsynchronousChannelGroup;

        abstract isShutdown(): boolean;

        abstract isTerminated(): boolean;

        abstract shutdown(): void;

        abstract shutdownNow(): void;

        abstract awaitTermination(arg0: number, arg1: TimeUnit): boolean;
    }

    export class AsynchronousCloseException extends ClosedChannelException {
        constructor();
    }

    export abstract class AsynchronousFileChannel implements AsynchronousChannel {

        static open(arg0: Path, arg1: Set<OpenOption>, arg2: ExecutorService, arg3: FileAttribute<any>[]): AsynchronousFileChannel;

        static open(arg0: Path, arg1: OpenOption[]): AsynchronousFileChannel;

        abstract size(): number;

        abstract truncate(arg0: number): AsynchronousFileChannel;

        abstract force(arg0: boolean): void;

        abstract lock<A extends Object>(arg0: number, arg1: number, arg2: boolean, arg3: A, arg4: CompletionHandler<FileLock, A>): void;

        lock<A extends Object>(arg0: A, arg1: CompletionHandler<FileLock, A>): void;

        abstract lock(arg0: number, arg1: number, arg2: boolean): Future<FileLock>;

        lock(): Future<FileLock>;

        abstract tryLock(arg0: number, arg1: number, arg2: boolean): FileLock;

        tryLock(): FileLock;

        abstract read<A extends Object>(arg0: ByteBuffer, arg1: number, arg2: A, arg3: CompletionHandler<Number, A>): void;

        abstract read(arg0: ByteBuffer, arg1: number): Future<Number>;

        abstract write<A extends Object>(arg0: ByteBuffer, arg1: number, arg2: A, arg3: CompletionHandler<Number, A>): void;

        abstract write(arg0: ByteBuffer, arg1: number): Future<Number>;
    }

    export abstract class AsynchronousServerSocketChannel implements AsynchronousChannel, NetworkChannel {

        provider(): AsynchronousChannelProvider;

        static open(arg0: AsynchronousChannelGroup): AsynchronousServerSocketChannel;

        static open(): AsynchronousServerSocketChannel;

        bind(arg0: SocketAddress): AsynchronousServerSocketChannel;

        abstract bind(arg0: SocketAddress, arg1: number): AsynchronousServerSocketChannel;

        abstract setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): AsynchronousServerSocketChannel;

        abstract accept<A extends Object>(arg0: A, arg1: CompletionHandler<AsynchronousSocketChannel, A>): void;

        abstract accept(): Future<AsynchronousSocketChannel>;

        abstract getLocalAddress(): SocketAddress;
    }

    export abstract class AsynchronousSocketChannel implements AsynchronousByteChannel, NetworkChannel {

        provider(): AsynchronousChannelProvider;

        static open(arg0: AsynchronousChannelGroup): AsynchronousSocketChannel;

        static open(): AsynchronousSocketChannel;

        abstract bind(arg0: SocketAddress): AsynchronousSocketChannel;

        abstract setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): AsynchronousSocketChannel;

        abstract shutdownInput(): AsynchronousSocketChannel;

        abstract shutdownOutput(): AsynchronousSocketChannel;

        abstract getRemoteAddress(): SocketAddress;

        abstract connect<A extends Object>(arg0: SocketAddress, arg1: A, arg2: CompletionHandler<Void, A>): void;

        abstract connect(arg0: SocketAddress): Future<Void>;

        abstract read<A extends Object>(arg0: ByteBuffer, arg1: number, arg2: TimeUnit, arg3: A, arg4: CompletionHandler<Number, A>): void;

        read<A extends Object>(arg0: ByteBuffer, arg1: A, arg2: CompletionHandler<Number, A>): void;

        abstract read(arg0: ByteBuffer): Future<Number>;

        abstract read<A extends Object>(arg0: ByteBuffer[], arg1: number, arg2: number, arg3: number, arg4: TimeUnit, arg5: A, arg6: CompletionHandler<Number, A>): void;

        abstract write<A extends Object>(arg0: ByteBuffer, arg1: number, arg2: TimeUnit, arg3: A, arg4: CompletionHandler<Number, A>): void;

        write<A extends Object>(arg0: ByteBuffer, arg1: A, arg2: CompletionHandler<Number, A>): void;

        abstract write(arg0: ByteBuffer): Future<Number>;

        abstract write<A extends Object>(arg0: ByteBuffer[], arg1: number, arg2: number, arg3: number, arg4: TimeUnit, arg5: A, arg6: CompletionHandler<Number, A>): void;

        abstract getLocalAddress(): SocketAddress;
    }

    export interface ByteChannel extends ReadableByteChannel, WritableByteChannel {
    }

    export class CancelledKeyException extends IllegalStateException {
        constructor();
    }

    export interface Channel extends Closeable {

        isOpen(): boolean;

        close(): void;
    }

    export class Channels {

        static newInputStream(arg0: ReadableByteChannel): InputStream;

        static newOutputStream(arg0: WritableByteChannel): OutputStream;

        static newInputStream(arg0: AsynchronousByteChannel): InputStream;

        static newOutputStream(arg0: AsynchronousByteChannel): OutputStream;

        static newChannel(arg0: InputStream): ReadableByteChannel;

        static newChannel(arg0: OutputStream): WritableByteChannel;

        static newReader(arg0: ReadableByteChannel, arg1: CharsetDecoder, arg2: number): Reader;

        static newReader(arg0: ReadableByteChannel, arg1: String): Reader;

        static newReader(arg0: ReadableByteChannel, arg1: Charset): Reader;

        static newWriter(arg0: WritableByteChannel, arg1: CharsetEncoder, arg2: number): Writer;

        static newWriter(arg0: WritableByteChannel, arg1: String): Writer;

        static newWriter(arg0: WritableByteChannel, arg1: Charset): Writer;
    }

    export class ClosedByInterruptException extends AsynchronousCloseException {
        constructor();
    }

    export class ClosedChannelException extends IOException {
        constructor();
    }

    export class ClosedSelectorException extends IllegalStateException {
        constructor();
    }

    export interface CompletionHandler<V extends Object, A extends Object> extends Object {

        completed(arg0: V, arg1: A): void;

        failed(arg0: Throwable, arg1: A): void;
    }

    export class ConnectionPendingException extends IllegalStateException {
        constructor();
    }

    export abstract class DatagramChannel extends AbstractSelectableChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, MulticastChannel {

        static open(): DatagramChannel;

        static open(arg0: ProtocolFamily): DatagramChannel;

        validOps(): number;

        abstract bind(arg0: SocketAddress): DatagramChannel;

        abstract setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): DatagramChannel;

        abstract socket(): DatagramSocket;

        abstract isConnected(): boolean;

        abstract connect(arg0: SocketAddress): DatagramChannel;

        abstract disconnect(): DatagramChannel;

        abstract getRemoteAddress(): SocketAddress;

        abstract receive(arg0: ByteBuffer): SocketAddress;

        abstract send(arg0: ByteBuffer, arg1: SocketAddress): number;

        abstract read(arg0: ByteBuffer): number;

        abstract read(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        read(arg0: ByteBuffer[]): number;

        abstract write(arg0: ByteBuffer): number;

        abstract write(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        write(arg0: ByteBuffer[]): number;

        abstract getLocalAddress(): SocketAddress;
    }

    export abstract class FileChannel extends AbstractInterruptibleChannel implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel {

        static open(arg0: Path, arg1: Set<OpenOption>, arg2: FileAttribute<any>[]): FileChannel;

        static open(arg0: Path, arg1: OpenOption[]): FileChannel;

        abstract read(arg0: ByteBuffer): number;

        abstract read(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        read(arg0: ByteBuffer[]): number;

        abstract write(arg0: ByteBuffer): number;

        abstract write(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        write(arg0: ByteBuffer[]): number;

        abstract position(): number;

        abstract position(arg0: number): FileChannel;

        abstract size(): number;

        abstract truncate(arg0: number): FileChannel;

        abstract force(arg0: boolean): void;

        abstract transferTo(arg0: number, arg1: number, arg2: WritableByteChannel): number;

        abstract transferFrom(arg0: ReadableByteChannel, arg1: number, arg2: number): number;

        abstract read(arg0: ByteBuffer, arg1: number): number;

        abstract write(arg0: ByteBuffer, arg1: number): number;

        abstract map(arg0: FileChannel.MapMode, arg1: number, arg2: number): MappedByteBuffer;

        abstract lock(arg0: number, arg1: number, arg2: boolean): FileLock;

        lock(): FileLock;

        abstract tryLock(arg0: number, arg1: number, arg2: boolean): FileLock;

        tryLock(): FileLock;
    }
    export namespace FileChannel {
        export class MapMode {
            static READ_ONLY: FileChannel.MapMode
            static READ_WRITE: FileChannel.MapMode
            static PRIVATE: FileChannel.MapMode
            toString(): string;
        }

    }

    export abstract class FileLock implements AutoCloseable {

        channel(): FileChannel;

        acquiredBy(): Channel;

        position(): number;

        size(): number;

        isShared(): boolean;

        overlaps(arg0: number, arg1: number): boolean;

        abstract isValid(): boolean;

        abstract release(): void;

        close(): void;
        toString(): string;
    }

    export class FileLockInterruptionException extends IOException {
        constructor();
    }

    export interface GatheringByteChannel extends WritableByteChannel {

        write(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        write(arg0: ByteBuffer[]): number;
    }

    export class IllegalBlockingModeException extends IllegalStateException {
        constructor();
    }

    export class IllegalChannelGroupException extends IllegalArgumentException {
        constructor();
    }

    export class IllegalSelectorException extends IllegalArgumentException {
        constructor();
    }

    export class InterruptedByTimeoutException extends IOException {
        constructor();
    }

    export interface InterruptibleChannel extends Channel {

        close(): void;
    }

    export abstract class MembershipKey {

        abstract isValid(): boolean;

        abstract drop(): void;

        abstract block(arg0: InetAddress): MembershipKey;

        abstract unblock(arg0: InetAddress): MembershipKey;

        abstract channel(): MulticastChannel;

        abstract group(): InetAddress;

        abstract networkInterface(): NetworkInterface;

        abstract sourceAddress(): InetAddress;
    }

    export interface MulticastChannel extends NetworkChannel {

        close(): void;

        join(arg0: InetAddress, arg1: NetworkInterface): MembershipKey;

        join(arg0: InetAddress, arg1: NetworkInterface, arg2: InetAddress): MembershipKey;
    }

    export interface NetworkChannel extends Channel {

        bind(arg0: SocketAddress): NetworkChannel;

        getLocalAddress(): SocketAddress;

        setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): NetworkChannel;

        getOption<T extends Object>(arg0: SocketOption<T>): T;

        supportedOptions(): Set<SocketOption<any>>;
    }

    export class NoConnectionPendingException extends IllegalStateException {
        constructor();
    }

    export class NonReadableChannelException extends IllegalStateException {
        constructor();
    }

    export class NonWritableChannelException extends IllegalStateException {
        constructor();
    }

    export class NotYetBoundException extends IllegalStateException {
        constructor();
    }

    export class NotYetConnectedException extends IllegalStateException {
        constructor();
    }

    export class OverlappingFileLockException extends IllegalStateException {
        constructor();
    }

    export abstract class Pipe {

        abstract source(): Pipe.SourceChannel;

        abstract sink(): Pipe.SinkChannel;

        static open(): Pipe;
    }
    export namespace Pipe {
        export abstract class SinkChannel extends AbstractSelectableChannel implements WritableByteChannel, GatheringByteChannel {

            validOps(): number;
        }

        export abstract class SourceChannel extends AbstractSelectableChannel implements ReadableByteChannel, ScatteringByteChannel {

            validOps(): number;
        }

    }

    export class ReadPendingException extends IllegalStateException {
        constructor();
    }

    export interface ReadableByteChannel extends Channel {

        read(arg0: ByteBuffer): number;
    }

    export interface ScatteringByteChannel extends ReadableByteChannel {

        read(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        read(arg0: ByteBuffer[]): number;
    }

    export interface SeekableByteChannel extends ByteChannel {

        read(arg0: ByteBuffer): number;

        write(arg0: ByteBuffer): number;

        position(): number;

        position(arg0: number): SeekableByteChannel;

        size(): number;

        truncate(arg0: number): SeekableByteChannel;
    }

    export abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

        abstract provider(): SelectorProvider;

        abstract validOps(): number;

        abstract isRegistered(): boolean;

        abstract keyFor(arg0: Selector): SelectionKey;

        abstract register(arg0: Selector, arg1: number, arg2: Object): SelectionKey;

        register(arg0: Selector, arg1: number): SelectionKey;

        abstract configureBlocking(arg0: boolean): SelectableChannel;

        abstract isBlocking(): boolean;

        abstract blockingLock(): Object;
    }

    export abstract class SelectionKey {
        static OP_READ: number
        static OP_WRITE: number
        static OP_CONNECT: number
        static OP_ACCEPT: number

        abstract channel(): SelectableChannel;

        abstract selector(): Selector;

        abstract isValid(): boolean;

        abstract cancel(): void;

        abstract interestOps(): number;

        abstract interestOps(arg0: number): SelectionKey;

        interestOpsOr(arg0: number): number;

        interestOpsAnd(arg0: number): number;

        abstract readyOps(): number;

        isReadable(): boolean;

        isWritable(): boolean;

        isConnectable(): boolean;

        isAcceptable(): boolean;

        attach(arg0: Object): Object;

        attachment(): Object;
    }

    export abstract class Selector implements Closeable {

        static open(): Selector;

        abstract isOpen(): boolean;

        abstract provider(): SelectorProvider;

        abstract keys(): Set<SelectionKey>;

        abstract selectedKeys(): Set<SelectionKey>;

        abstract selectNow(): number;

        abstract select(arg0: number): number;

        abstract select(): number;

        select(arg0: Consumer<SelectionKey>, arg1: number): number;

        select(arg0: Consumer<SelectionKey>): number;

        selectNow(arg0: Consumer<SelectionKey>): number;

        abstract wakeup(): Selector;

        abstract close(): void;
    }

    export abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel {

        static open(): ServerSocketChannel;

        static open(arg0: ProtocolFamily): ServerSocketChannel;

        validOps(): number;

        bind(arg0: SocketAddress): ServerSocketChannel;

        abstract bind(arg0: SocketAddress, arg1: number): ServerSocketChannel;

        abstract setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): ServerSocketChannel;

        abstract socket(): ServerSocket;

        abstract accept(): SocketChannel;

        abstract getLocalAddress(): SocketAddress;
    }

    export class ShutdownChannelGroupException extends IllegalStateException {
        constructor();
    }

    export abstract class SocketChannel extends AbstractSelectableChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {

        static open(): SocketChannel;

        static open(arg0: ProtocolFamily): SocketChannel;

        static open(arg0: SocketAddress): SocketChannel;

        validOps(): number;

        abstract bind(arg0: SocketAddress): SocketChannel;

        abstract setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): SocketChannel;

        abstract shutdownInput(): SocketChannel;

        abstract shutdownOutput(): SocketChannel;

        abstract socket(): Socket;

        abstract isConnected(): boolean;

        abstract isConnectionPending(): boolean;

        abstract connect(arg0: SocketAddress): boolean;

        abstract finishConnect(): boolean;

        abstract getRemoteAddress(): SocketAddress;

        abstract read(arg0: ByteBuffer): number;

        abstract read(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        read(arg0: ByteBuffer[]): number;

        abstract write(arg0: ByteBuffer): number;

        abstract write(arg0: ByteBuffer[], arg1: number, arg2: number): number;

        write(arg0: ByteBuffer[]): number;

        abstract getLocalAddress(): SocketAddress;
    }

    export class UnresolvedAddressException extends IllegalArgumentException {
        constructor();
    }

    export class UnsupportedAddressTypeException extends IllegalArgumentException {
        constructor();
    }

    export interface WritableByteChannel extends Channel {

        write(arg0: ByteBuffer): number;
    }

    export class WritePendingException extends IllegalStateException {
        constructor();
    }

}
/// <reference path="java.net.d.ts" />
declare module '@java/java.net.spi' {
    import { URLStreamHandlerFactory } from '@java/java.net'
    export abstract class URLStreamHandlerProvider implements URLStreamHandlerFactory {
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="javax.net.ssl.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.net.http' {
    import { OptionalLong, List, Optional, Map } from '@java/java.util'
    import { Duration } from '@java/java.time'
    import { Enum, Iterable, CharSequence, Throwable, Class, String, Void } from '@java/java.lang'
    import { ProxySelector, URI, Authenticator, CookieHandler } from '@java/java.net'
    import { InputStream, IOException } from '@java/java.io'
    import { ConcurrentMap, CompletableFuture, Executor, CompletionStage, Flow } from '@java/java.util.concurrent'
    import { Stream } from '@java/java.util.stream'
    import { ByteBuffer } from '@java/java.nio'
    import { SSLParameters, SSLSession, SSLContext } from '@java/javax.net.ssl'
    import { Function, Supplier, Consumer, BiPredicate } from '@java/java.util.function'
    import { Path, OpenOption } from '@java/java.nio.file'
    import { Charset } from '@java/java.nio.charset'
    export abstract class HttpClient {

        static newHttpClient(): HttpClient;

        static newBuilder(): HttpClient.Builder;

        abstract cookieHandler(): Optional<CookieHandler>;

        abstract connectTimeout(): Optional<Duration>;

        abstract followRedirects(): HttpClient.Redirect;

        abstract proxy(): Optional<ProxySelector>;

        abstract sslContext(): SSLContext;

        abstract sslParameters(): SSLParameters;

        abstract authenticator(): Optional<Authenticator>;

        abstract version(): HttpClient.Version;

        abstract executor(): Optional<Executor>;

        abstract send<T extends Object>(arg0: HttpRequest, arg1: HttpResponse.BodyHandler<T>): HttpResponse<T>;

        abstract sendAsync<T extends Object>(arg0: HttpRequest, arg1: HttpResponse.BodyHandler<T>): CompletableFuture<HttpResponse<T>>;

        abstract sendAsync<T extends Object>(arg0: HttpRequest, arg1: HttpResponse.BodyHandler<T>, arg2: HttpResponse.PushPromiseHandler<T>): CompletableFuture<HttpResponse<T>>;

        newWebSocketBuilder(): WebSocket.Builder;
    }
    export namespace HttpClient {
        export namespace Builder {
            const NO_PROXY: ProxySelector
        }

        export interface Builder {
            NO_PROXY: ProxySelector

            cookieHandler(arg0: CookieHandler): HttpClient.Builder;

            connectTimeout(arg0: Duration): HttpClient.Builder;

            sslContext(arg0: SSLContext): HttpClient.Builder;

            sslParameters(arg0: SSLParameters): HttpClient.Builder;

            executor(arg0: Executor): HttpClient.Builder;

            followRedirects(arg0: HttpClient.Redirect): HttpClient.Builder;

            version(arg0: HttpClient.Version): HttpClient.Builder;

            priority(arg0: number): HttpClient.Builder;

            proxy(arg0: ProxySelector): HttpClient.Builder;

            authenticator(arg0: Authenticator): HttpClient.Builder;

            build(): HttpClient;
        }

        export class Redirect extends Enum<HttpClient.Redirect> {
            static NEVER: HttpClient.Redirect
            static ALWAYS: HttpClient.Redirect
            static NORMAL: HttpClient.Redirect

            static values(): HttpClient.Redirect[];

            static valueOf(arg0: String): HttpClient.Redirect;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Version extends Enum<HttpClient.Version> {
            static HTTP_1_1: HttpClient.Version
            static HTTP_2: HttpClient.Version

            static values(): HttpClient.Version[];

            static valueOf(arg0: String): HttpClient.Version;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class HttpConnectTimeoutException extends HttpTimeoutException {
        constructor(arg0: String);
    }

    export class HttpHeaders {

        firstValue(arg0: String): Optional<String>;

        firstValueAsLong(arg0: String): OptionalLong;

        allValues(arg0: String): List<String>;

        map(): Map<String, List<String>>;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        static of(arg0: Map<String, List<String>>, arg1: BiPredicate<String, String>): HttpHeaders;
    }

    export abstract class HttpRequest {

        static newBuilder(arg0: URI): HttpRequest.Builder;

        static newBuilder(arg0: HttpRequest, arg1: BiPredicate<String, String>): HttpRequest.Builder;

        static newBuilder(): HttpRequest.Builder;

        abstract bodyPublisher(): Optional<HttpRequest.BodyPublisher>;

        abstract method(): String;

        abstract timeout(): Optional<Duration>;

        abstract expectContinue(): boolean;

        abstract uri(): URI;

        abstract version(): Optional<HttpClient.Version>;

        abstract headers(): HttpHeaders;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }
    export namespace HttpRequest {
        export interface BodyPublisher extends Flow.Publisher<ByteBuffer>, Object {

            contentLength(): number;
        }

        export class BodyPublishers {

            static fromPublisher(arg0: Flow.Publisher<ByteBuffer>): HttpRequest.BodyPublisher;

            static fromPublisher(arg0: Flow.Publisher<ByteBuffer>, arg1: number): HttpRequest.BodyPublisher;

            static ofString(arg0: String): HttpRequest.BodyPublisher;

            static ofString(arg0: String, arg1: Charset): HttpRequest.BodyPublisher;

            static ofInputStream(arg0: Supplier<InputStream>): HttpRequest.BodyPublisher;

            static ofByteArray(arg0: number[]): HttpRequest.BodyPublisher;

            static ofByteArray(arg0: number[], arg1: number, arg2: number): HttpRequest.BodyPublisher;

            static ofFile(arg0: Path): HttpRequest.BodyPublisher;

            static ofByteArrays(arg0: Iterable<number[]>): HttpRequest.BodyPublisher;

            static noBody(): HttpRequest.BodyPublisher;

            static concat(arg0: HttpRequest.BodyPublisher[]): HttpRequest.BodyPublisher;
        }

        export interface Builder {

            uri(arg0: URI): HttpRequest.Builder;

            expectContinue(arg0: boolean): HttpRequest.Builder;

            version(arg0: HttpClient.Version): HttpRequest.Builder;

            header(arg0: String, arg1: String): HttpRequest.Builder;

            headers(arg0: String[]): HttpRequest.Builder;

            timeout(arg0: Duration): HttpRequest.Builder;

            setHeader(arg0: String, arg1: String): HttpRequest.Builder;

            GET(): HttpRequest.Builder;

            POST(arg0: HttpRequest.BodyPublisher): HttpRequest.Builder;

            PUT(arg0: HttpRequest.BodyPublisher): HttpRequest.Builder;

            DELETE(): HttpRequest.Builder;

            method(arg0: String, arg1: HttpRequest.BodyPublisher): HttpRequest.Builder;

            build(): HttpRequest;

            copy(): HttpRequest.Builder;
        }

    }

    export interface HttpResponse<T extends Object> extends Object {

        statusCode(): number;

        request(): HttpRequest;

        previousResponse(): Optional<HttpResponse<T>>;

        headers(): HttpHeaders;

        body(): T;

        sslSession(): Optional<SSLSession>;

        uri(): URI;

        version(): HttpClient.Version;
    }
    export namespace HttpResponse {
        export interface BodyHandler<T extends Object> extends Object {

            apply(arg0: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<T>;
        }

        export class BodyHandlers {

            static fromSubscriber(arg0: Flow.Subscriber<List<ByteBuffer>>): HttpResponse.BodyHandler<Void>;

            static fromSubscriber<S extends Flow.Subscriber<List<ByteBuffer>>, T extends Object>(arg0: S, arg1: Function<S, T>): HttpResponse.BodyHandler<T>;

            static fromLineSubscriber(arg0: Flow.Subscriber<String>): HttpResponse.BodyHandler<Void>;

            static fromLineSubscriber<S extends Flow.Subscriber<String>, T extends Object>(arg0: S, arg1: Function<S, T>, arg2: String): HttpResponse.BodyHandler<T>;

            static discarding(): HttpResponse.BodyHandler<Void>;

            static replacing<U extends Object>(arg0: U): HttpResponse.BodyHandler<U>;

            static ofString(arg0: Charset): HttpResponse.BodyHandler<String>;

            static ofFile(arg0: Path, arg1: OpenOption[]): HttpResponse.BodyHandler<Path>;

            static ofFile(arg0: Path): HttpResponse.BodyHandler<Path>;

            static ofFileDownload(arg0: Path, arg1: OpenOption[]): HttpResponse.BodyHandler<Path>;

            static ofInputStream(): HttpResponse.BodyHandler<InputStream>;

            static ofLines(): HttpResponse.BodyHandler<Stream<String>>;

            static ofByteArrayConsumer(arg0: Consumer<Optional<number[]>>): HttpResponse.BodyHandler<Void>;

            static ofByteArray(): HttpResponse.BodyHandler<number[]>;

            static ofString(): HttpResponse.BodyHandler<String>;

            static ofPublisher(): HttpResponse.BodyHandler<Flow.Publisher<List<ByteBuffer>>>;

            static buffering<T extends Object>(arg0: HttpResponse.BodyHandler<T>, arg1: number): HttpResponse.BodyHandler<T>;
        }

        export interface BodySubscriber<T extends Object> extends Flow.Subscriber<List<ByteBuffer>>, Object {

            getBody(): CompletionStage<T>;
        }

        export class BodySubscribers {

            static fromSubscriber(arg0: Flow.Subscriber<List<ByteBuffer>>): HttpResponse.BodySubscriber<Void>;

            static fromSubscriber<S extends Flow.Subscriber<List<ByteBuffer>>, T extends Object>(arg0: S, arg1: Function<S, T>): HttpResponse.BodySubscriber<T>;

            static fromLineSubscriber(arg0: Flow.Subscriber<String>): HttpResponse.BodySubscriber<Void>;

            static fromLineSubscriber<S extends Flow.Subscriber<String>, T extends Object>(arg0: S, arg1: Function<S, T>, arg2: Charset, arg3: String): HttpResponse.BodySubscriber<T>;

            static ofString(arg0: Charset): HttpResponse.BodySubscriber<String>;

            static ofByteArray(): HttpResponse.BodySubscriber<number[]>;

            static ofFile(arg0: Path, arg1: OpenOption[]): HttpResponse.BodySubscriber<Path>;

            static ofFile(arg0: Path): HttpResponse.BodySubscriber<Path>;

            static ofByteArrayConsumer(arg0: Consumer<Optional<number[]>>): HttpResponse.BodySubscriber<Void>;

            static ofInputStream(): HttpResponse.BodySubscriber<InputStream>;

            static ofLines(arg0: Charset): HttpResponse.BodySubscriber<Stream<String>>;

            static ofPublisher(): HttpResponse.BodySubscriber<Flow.Publisher<List<ByteBuffer>>>;

            static replacing<U extends Object>(arg0: U): HttpResponse.BodySubscriber<U>;

            static discarding(): HttpResponse.BodySubscriber<Void>;

            static buffering<T extends Object>(arg0: HttpResponse.BodySubscriber<T>, arg1: number): HttpResponse.BodySubscriber<T>;

            static mapping<T extends Object, U extends Object>(arg0: HttpResponse.BodySubscriber<T>, arg1: Function<T, U>): HttpResponse.BodySubscriber<U>;
        }

        export namespace PushPromiseHandler {
            function
/* default */ of<T extends Object>(arg0: Function<HttpRequest, HttpResponse.BodyHandler<T>>, arg1: ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<T>>>): HttpResponse.PushPromiseHandler<T>;
        }

        export interface PushPromiseHandler<T extends Object> extends Object {

            applyPushPromise(arg0: HttpRequest, arg1: HttpRequest, arg2: Function<HttpResponse.BodyHandler<T>, CompletableFuture<HttpResponse<T>>>): void;
        }

        export interface ResponseInfo {

            statusCode(): number;

            headers(): HttpHeaders;

            version(): HttpClient.Version;
        }

    }

    export class HttpTimeoutException extends IOException {
        constructor(arg0: String);
    }

    export namespace WebSocket {
        const NORMAL_CLOSURE: number
    }

    export interface WebSocket {
        NORMAL_CLOSURE: number

        sendText(arg0: CharSequence, arg1: boolean): CompletableFuture<WebSocket>;

        sendBinary(arg0: ByteBuffer, arg1: boolean): CompletableFuture<WebSocket>;

        sendPing(arg0: ByteBuffer): CompletableFuture<WebSocket>;

        sendPong(arg0: ByteBuffer): CompletableFuture<WebSocket>;

        sendClose(arg0: number, arg1: String): CompletableFuture<WebSocket>;

        request(arg0: number): void;

        getSubprotocol(): String;

        isOutputClosed(): boolean;

        isInputClosed(): boolean;

        abort(): void;
    }
    export namespace WebSocket {
        export interface Builder {

            header(arg0: String, arg1: String): WebSocket.Builder;

            connectTimeout(arg0: Duration): WebSocket.Builder;

            subprotocols(arg0: String, arg1: String[]): WebSocket.Builder;

            buildAsync(arg0: URI, arg1: WebSocket.Listener): CompletableFuture<WebSocket>;
        }

        export interface Listener {

/* default */ onOpen(arg0: WebSocket): void;

/* default */ onText(arg0: WebSocket, arg1: CharSequence, arg2: boolean): CompletionStage<any>;

/* default */ onBinary(arg0: WebSocket, arg1: ByteBuffer, arg2: boolean): CompletionStage<any>;

/* default */ onPing(arg0: WebSocket, arg1: ByteBuffer): CompletionStage<any>;

/* default */ onPong(arg0: WebSocket, arg1: ByteBuffer): CompletionStage<any>;

/* default */ onClose(arg0: WebSocket, arg1: number, arg2: String): CompletionStage<any>;

/* default */ onError(arg0: WebSocket, arg1: Throwable): void;
        }

    }

    export class WebSocketHandshakeException extends IOException {
        constructor(arg0: HttpResponse<any>);

        getResponse(): HttpResponse<any>;

        initCause(arg0: Throwable): WebSocketHandshakeException;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.jar.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="javax.net.ssl.d.ts" />
/// <reference path="java.security.cert.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.net' {
    import { Permission, BasicPermission, PermissionCollection, SecureClassLoader, Principal } from '@java/java.security'
    import { Enum, Integer, Comparable, ClassLoader, Class, Cloneable, String, Boolean, Exception } from '@java/java.lang'
    import { List, Set, Enumeration, Optional, Map } from '@java/java.util'
    import { InterruptedIOException, Serializable, IOException, InputStream, OutputStream, Closeable } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { Attributes, JarFile, Manifest, JarEntry } from '@java/java.util.jar'
    import { DatagramChannel, SocketChannel, ServerSocketChannel } from '@java/java.nio.channels'
    import { SSLSession } from '@java/javax.net.ssl'
    import { Certificate } from '@java/java.security.cert'
    import { Path } from '@java/java.nio.file'
    import { Charset } from '@java/java.nio.charset'
    export abstract class Authenticator {
        constructor();

        static setDefault(arg0: Authenticator): void;

        static getDefault(): Authenticator;

        static requestPasswordAuthentication(arg0: InetAddress, arg1: number, arg2: String, arg3: String, arg4: String): PasswordAuthentication;

        static requestPasswordAuthentication(arg0: String, arg1: InetAddress, arg2: number, arg3: String, arg4: String, arg5: String): PasswordAuthentication;

        static requestPasswordAuthentication(arg0: String, arg1: InetAddress, arg2: number, arg3: String, arg4: String, arg5: String, arg6: URL, arg7: Authenticator.RequestorType): PasswordAuthentication;

        static requestPasswordAuthentication(arg0: Authenticator, arg1: String, arg2: InetAddress, arg3: number, arg4: String, arg5: String, arg6: String, arg7: URL, arg8: Authenticator.RequestorType): PasswordAuthentication;

        requestPasswordAuthenticationInstance(arg0: String, arg1: InetAddress, arg2: number, arg3: String, arg4: String, arg5: String, arg6: URL, arg7: Authenticator.RequestorType): PasswordAuthentication;
    }
    export namespace Authenticator {
        export class RequestorType extends Enum<Authenticator.RequestorType> {
            static PROXY: Authenticator.RequestorType
            static SERVER: Authenticator.RequestorType

            static values(): Authenticator.RequestorType[];

            static valueOf(arg0: String): Authenticator.RequestorType;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class BindException extends SocketException {
        constructor(arg0: String);
        constructor();
    }

    export abstract class CacheRequest {
        constructor();

        abstract getBody(): OutputStream;

        abstract abort(): void;
    }

    export abstract class CacheResponse {
        constructor();

        abstract getHeaders(): Map<String, List<String>>;

        abstract getBody(): InputStream;
    }

    export class ConnectException extends SocketException {
        constructor(arg0: String);
        constructor();
    }

    export abstract class ContentHandler {
        constructor();

        abstract getContent(arg0: URLConnection): Object;

        getContent(arg0: URLConnection, arg1: Class[]): Object;
    }

    export interface ContentHandlerFactory {

        createContentHandler(arg0: String): ContentHandler;
    }

    export abstract class CookieHandler {
        constructor();

        static getDefault(): CookieHandler;

        static setDefault(arg0: CookieHandler): void;

        abstract get(arg0: URI, arg1: Map<String, List<String>>): Map<String, List<String>>;

        abstract put(arg0: URI, arg1: Map<String, List<String>>): void;
    }

    export class CookieManager extends CookieHandler {
        constructor();
        constructor(arg0: CookieStore, arg1: CookiePolicy);

        setCookiePolicy(arg0: CookiePolicy): void;

        getCookieStore(): CookieStore;

        get(arg0: URI, arg1: Map<String, List<String>>): Map<String, List<String>>;

        put(arg0: URI, arg1: Map<String, List<String>>): void;
    }

    export namespace CookiePolicy {
        const ACCEPT_ALL: CookiePolicy
        const ACCEPT_NONE: CookiePolicy
        const ACCEPT_ORIGINAL_SERVER: CookiePolicy
    }

    export interface CookiePolicy {
        ACCEPT_ALL: CookiePolicy
        ACCEPT_NONE: CookiePolicy
        ACCEPT_ORIGINAL_SERVER: CookiePolicy

        shouldAccept(arg0: URI, arg1: HttpCookie): boolean;
    }

    export interface CookieStore {

        add(arg0: URI, arg1: HttpCookie): void;

        get(arg0: URI): List<HttpCookie>;

        getCookies(): List<HttpCookie>;

        getURIs(): List<URI>;

        remove(arg0: URI, arg1: HttpCookie): boolean;

        removeAll(): boolean;
    }

    export class DatagramPacket {
        constructor(arg0: number[], arg1: number, arg2: number);
        constructor(arg0: number[], arg1: number);
        constructor(arg0: number[], arg1: number, arg2: number, arg3: InetAddress, arg4: number);
        constructor(arg0: number[], arg1: number, arg2: number, arg3: SocketAddress);
        constructor(arg0: number[], arg1: number, arg2: InetAddress, arg3: number);
        constructor(arg0: number[], arg1: number, arg2: SocketAddress);

        getAddress(): InetAddress;

        getPort(): number;

        getData(): number[];

        getOffset(): number;

        getLength(): number;

        setData(arg0: number[], arg1: number, arg2: number): void;

        setAddress(arg0: InetAddress): void;

        setPort(arg0: number): void;

        setSocketAddress(arg0: SocketAddress): void;

        getSocketAddress(): SocketAddress;

        setData(arg0: number[]): void;

        setLength(arg0: number): void;
    }

    export class DatagramSocket implements Closeable {
        constructor();
        constructor(arg0: SocketAddress);
        constructor(arg0: number);
        constructor(arg0: number, arg1: InetAddress);

        bind(arg0: SocketAddress): void;

        connect(arg0: InetAddress, arg1: number): void;

        connect(arg0: SocketAddress): void;

        disconnect(): void;

        isBound(): boolean;

        isConnected(): boolean;

        getInetAddress(): InetAddress;

        getPort(): number;

        getRemoteSocketAddress(): SocketAddress;

        getLocalSocketAddress(): SocketAddress;

        send(arg0: DatagramPacket): void;

        receive(arg0: DatagramPacket): void;

        getLocalAddress(): InetAddress;

        getLocalPort(): number;

        setSoTimeout(arg0: number): void;

        getSoTimeout(): number;

        setSendBufferSize(arg0: number): void;

        getSendBufferSize(): number;

        setReceiveBufferSize(arg0: number): void;

        getReceiveBufferSize(): number;

        setReuseAddress(arg0: boolean): void;

        getReuseAddress(): boolean;

        setBroadcast(arg0: boolean): void;

        getBroadcast(): boolean;

        setTrafficClass(arg0: number): void;

        getTrafficClass(): number;

        close(): void;

        isClosed(): boolean;

        getChannel(): DatagramChannel;

        static setDatagramSocketImplFactory(arg0: DatagramSocketImplFactory): void;

        setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): DatagramSocket;

        getOption<T extends Object>(arg0: SocketOption<T>): T;

        supportedOptions(): Set<SocketOption<any>>;

        joinGroup(arg0: SocketAddress, arg1: NetworkInterface): void;

        leaveGroup(arg0: SocketAddress, arg1: NetworkInterface): void;
    }

    export abstract class DatagramSocketImpl implements SocketOptions {
        constructor();
    }

    export interface DatagramSocketImplFactory {

        createDatagramSocketImpl(): DatagramSocketImpl;
    }

    export interface FileNameMap {

        getContentTypeFor(arg0: String): String;
    }

    export class HttpCookie implements Cloneable {
        constructor(arg0: String, arg1: String);

        static parse(arg0: String): List<HttpCookie>;

        hasExpired(): boolean;

        setComment(arg0: String): void;

        getComment(): String;

        setCommentURL(arg0: String): void;

        getCommentURL(): String;

        setDiscard(arg0: boolean): void;

        getDiscard(): boolean;

        setPortlist(arg0: String): void;

        getPortlist(): String;

        setDomain(arg0: String): void;

        getDomain(): String;

        setMaxAge(arg0: number): void;

        getMaxAge(): number;

        setPath(arg0: String): void;

        getPath(): String;

        setSecure(arg0: boolean): void;

        getSecure(): boolean;

        getName(): String;

        setValue(arg0: String): void;

        getValue(): String;

        getVersion(): number;

        setVersion(arg0: number): void;

        isHttpOnly(): boolean;

        setHttpOnly(arg0: boolean): void;

        static domainMatches(arg0: String, arg1: String): boolean;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;

        clone(): Object;
    }

    export class HttpRetryException extends IOException {
        constructor(arg0: String, arg1: number);
        constructor(arg0: String, arg1: number, arg2: String);

        responseCode(): number;

        getReason(): String;

        getLocation(): String;
    }

    export abstract class HttpURLConnection extends URLConnection {
        static HTTP_OK: number
        static HTTP_CREATED: number
        static HTTP_ACCEPTED: number
        static HTTP_NOT_AUTHORITATIVE: number
        static HTTP_NO_CONTENT: number
        static HTTP_RESET: number
        static HTTP_PARTIAL: number
        static HTTP_MULT_CHOICE: number
        static HTTP_MOVED_PERM: number
        static HTTP_MOVED_TEMP: number
        static HTTP_SEE_OTHER: number
        static HTTP_NOT_MODIFIED: number
        static HTTP_USE_PROXY: number
        static HTTP_BAD_REQUEST: number
        static HTTP_UNAUTHORIZED: number
        static HTTP_PAYMENT_REQUIRED: number
        static HTTP_FORBIDDEN: number
        static HTTP_NOT_FOUND: number
        static HTTP_BAD_METHOD: number
        static HTTP_NOT_ACCEPTABLE: number
        static HTTP_PROXY_AUTH: number
        static HTTP_CLIENT_TIMEOUT: number
        static HTTP_CONFLICT: number
        static HTTP_GONE: number
        static HTTP_LENGTH_REQUIRED: number
        static HTTP_PRECON_FAILED: number
        static HTTP_ENTITY_TOO_LARGE: number
        static HTTP_REQ_TOO_LONG: number
        static HTTP_UNSUPPORTED_TYPE: number
        static HTTP_SERVER_ERROR: number
        static HTTP_INTERNAL_ERROR: number
        static HTTP_NOT_IMPLEMENTED: number
        static HTTP_BAD_GATEWAY: number
        static HTTP_UNAVAILABLE: number
        static HTTP_GATEWAY_TIMEOUT: number
        static HTTP_VERSION: number

        setAuthenticator(arg0: Authenticator): void;

        getHeaderFieldKey(arg0: number): String;

        setFixedLengthStreamingMode(arg0: number): void;

        setFixedLengthStreamingMode(arg0: number): void;

        setChunkedStreamingMode(arg0: number): void;

        getHeaderField(arg0: number): String;

        static setFollowRedirects(arg0: boolean): void;

        static getFollowRedirects(): boolean;

        setInstanceFollowRedirects(arg0: boolean): void;

        getInstanceFollowRedirects(): boolean;

        setRequestMethod(arg0: String): void;

        getRequestMethod(): String;

        getResponseCode(): number;

        getResponseMessage(): String;

        getHeaderFieldDate(arg0: String, arg1: number): number;

        abstract disconnect(): void;

        abstract usingProxy(): boolean;

        getPermission(): Permission;

        getErrorStream(): InputStream;
    }

    export class IDN {
        static ALLOW_UNASSIGNED: number
        static USE_STD3_ASCII_RULES: number

        static toASCII(arg0: String, arg1: number): String;

        static toASCII(arg0: String): String;

        static toUnicode(arg0: String, arg1: number): String;

        static toUnicode(arg0: String): String;
    }

    export class Inet4Address extends InetAddress {

        isMulticastAddress(): boolean;

        isAnyLocalAddress(): boolean;

        isLoopbackAddress(): boolean;

        isLinkLocalAddress(): boolean;

        isSiteLocalAddress(): boolean;

        isMCGlobal(): boolean;

        isMCNodeLocal(): boolean;

        isMCLinkLocal(): boolean;

        isMCSiteLocal(): boolean;

        isMCOrgLocal(): boolean;

        getAddress(): number[];

        getHostAddress(): String;

        hashCode(): number;

        equals(arg0: Object): boolean;
    }

    export class Inet6Address extends InetAddress {

        static getByAddress(arg0: String, arg1: number[], arg2: NetworkInterface): Inet6Address;

        static getByAddress(arg0: String, arg1: number[], arg2: number): Inet6Address;

        isMulticastAddress(): boolean;

        isAnyLocalAddress(): boolean;

        isLoopbackAddress(): boolean;

        isLinkLocalAddress(): boolean;

        isSiteLocalAddress(): boolean;

        isMCGlobal(): boolean;

        isMCNodeLocal(): boolean;

        isMCLinkLocal(): boolean;

        isMCSiteLocal(): boolean;

        isMCOrgLocal(): boolean;

        getAddress(): number[];

        getScopeId(): number;

        getScopedInterface(): NetworkInterface;

        getHostAddress(): String;

        hashCode(): number;

        equals(arg0: Object): boolean;

        isIPv4CompatibleAddress(): boolean;
    }

    export class InetAddress implements Serializable {

        isMulticastAddress(): boolean;

        isAnyLocalAddress(): boolean;

        isLoopbackAddress(): boolean;

        isLinkLocalAddress(): boolean;

        isSiteLocalAddress(): boolean;

        isMCGlobal(): boolean;

        isMCNodeLocal(): boolean;

        isMCLinkLocal(): boolean;

        isMCSiteLocal(): boolean;

        isMCOrgLocal(): boolean;

        isReachable(arg0: number): boolean;

        isReachable(arg0: NetworkInterface, arg1: number, arg2: number): boolean;

        getHostName(): String;

        getCanonicalHostName(): String;

        getAddress(): number[];

        getHostAddress(): String;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;

        static getByAddress(arg0: String, arg1: number[]): InetAddress;

        static getByName(arg0: String): InetAddress;

        static getAllByName(arg0: String): InetAddress[];

        static getLoopbackAddress(): InetAddress;

        static getByAddress(arg0: number[]): InetAddress;

        static getLocalHost(): InetAddress;
    }

    export class InetSocketAddress extends SocketAddress {
        constructor(arg0: number);
        constructor(arg0: InetAddress, arg1: number);
        constructor(arg0: String, arg1: number);

        static createUnresolved(arg0: String, arg1: number): InetSocketAddress;

        getPort(): number;

        getAddress(): InetAddress;

        getHostName(): String;

        getHostString(): String;

        isUnresolved(): boolean;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class InterfaceAddress {

        getAddress(): InetAddress;

        getBroadcast(): InetAddress;

        getNetworkPrefixLength(): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export abstract class JarURLConnection extends URLConnection {

        getJarFileURL(): URL;

        getEntryName(): String;

        abstract getJarFile(): JarFile;

        getManifest(): Manifest;

        getJarEntry(): JarEntry;

        getAttributes(): Attributes;

        getMainAttributes(): Attributes;

        getCertificates(): Certificate[];
    }

    export class MalformedURLException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export class MulticastSocket extends DatagramSocket {
        constructor();
        constructor(arg0: number);
        constructor(arg0: SocketAddress);

        setTTL(arg0: number): void;

        setTimeToLive(arg0: number): void;

        getTTL(): number;

        getTimeToLive(): number;

        joinGroup(arg0: InetAddress): void;

        leaveGroup(arg0: InetAddress): void;

        joinGroup(arg0: SocketAddress, arg1: NetworkInterface): void;

        leaveGroup(arg0: SocketAddress, arg1: NetworkInterface): void;

        setInterface(arg0: InetAddress): void;

        getInterface(): InetAddress;

        setNetworkInterface(arg0: NetworkInterface): void;

        getNetworkInterface(): NetworkInterface;

        setLoopbackMode(arg0: boolean): void;

        getLoopbackMode(): boolean;

        send(arg0: DatagramPacket, arg1: number): void;
    }

    export class NetPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export class NetworkInterface {

        getName(): String;

        getInetAddresses(): Enumeration<InetAddress>;

        inetAddresses(): Stream<InetAddress>;

        getInterfaceAddresses(): List<InterfaceAddress>;

        getSubInterfaces(): Enumeration<NetworkInterface>;

        subInterfaces(): Stream<NetworkInterface>;

        getParent(): NetworkInterface;

        getIndex(): number;

        getDisplayName(): String;

        static getByName(arg0: String): NetworkInterface;

        static getByIndex(arg0: number): NetworkInterface;

        static getByInetAddress(arg0: InetAddress): NetworkInterface;

        static getNetworkInterfaces(): Enumeration<NetworkInterface>;

        static networkInterfaces(): Stream<NetworkInterface>;

        isUp(): boolean;

        isLoopback(): boolean;

        isPointToPoint(): boolean;

        supportsMulticast(): boolean;

        getHardwareAddress(): number[];

        getMTU(): number;

        isVirtual(): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class NoRouteToHostException extends SocketException {
        constructor(arg0: String);
        constructor();
    }

    export class PasswordAuthentication {
        constructor(arg0: String, arg1: String[]);

        getUserName(): String;

        getPassword(): String[];
    }

    export class PortUnreachableException extends SocketException {
        constructor(arg0: String);
        constructor();
    }

    export class ProtocolException extends IOException {
        constructor(arg0: String);
        constructor();
    }

    export interface ProtocolFamily {

        name(): String;
    }

    export class Proxy {
        static NO_PROXY: Proxy
        constructor(arg0: Proxy.Type, arg1: SocketAddress);

        type(): Proxy.Type;

        address(): SocketAddress;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }
    export namespace Proxy {
        export class Type extends Enum<Proxy.Type> {
            static DIRECT: Proxy.Type
            static HTTP: Proxy.Type
            static SOCKS: Proxy.Type

            static values(): Proxy.Type[];

            static valueOf(arg0: String): Proxy.Type;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export abstract class ProxySelector {
        constructor();

        static getDefault(): ProxySelector;

        static setDefault(arg0: ProxySelector): void;

        abstract select(arg0: URI): List<Proxy>;

        abstract connectFailed(arg0: URI, arg1: SocketAddress, arg2: IOException): void;

        static of(arg0: InetSocketAddress): ProxySelector;
    }

    export abstract class ResponseCache {
        constructor();

        static getDefault(): ResponseCache;

        static setDefault(arg0: ResponseCache): void;

        abstract get(arg0: URI, arg1: String, arg2: Map<String, List<String>>): CacheResponse;

        abstract put(arg0: URI, arg1: URLConnection): CacheRequest;
    }

    export abstract class SecureCacheResponse extends CacheResponse {
        constructor();

        abstract getCipherSuite(): String;

        abstract getLocalCertificateChain(): List<Certificate>;

        abstract getServerCertificateChain(): List<Certificate>;

        abstract getPeerPrincipal(): Principal;

        abstract getLocalPrincipal(): Principal;

        getSSLSession(): Optional<SSLSession>;
    }

    export class ServerSocket implements Closeable {
        constructor();
        constructor(arg0: number);
        constructor(arg0: number, arg1: number);
        constructor(arg0: number, arg1: number, arg2: InetAddress);

        bind(arg0: SocketAddress): void;

        bind(arg0: SocketAddress, arg1: number): void;

        getInetAddress(): InetAddress;

        getLocalPort(): number;

        getLocalSocketAddress(): SocketAddress;

        accept(): Socket;

        close(): void;

        getChannel(): ServerSocketChannel;

        isBound(): boolean;

        isClosed(): boolean;

        setSoTimeout(arg0: number): void;

        getSoTimeout(): number;

        setReuseAddress(arg0: boolean): void;

        getReuseAddress(): boolean;
        toString(): string;

        static setSocketFactory(arg0: SocketImplFactory): void;

        setReceiveBufferSize(arg0: number): void;

        getReceiveBufferSize(): number;

        setPerformancePreferences(arg0: number, arg1: number, arg2: number): void;

        setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): ServerSocket;

        getOption<T extends Object>(arg0: SocketOption<T>): T;

        supportedOptions(): Set<SocketOption<any>>;
    }

    export class Socket implements Closeable {
        constructor();
        constructor(arg0: Proxy);
        constructor(arg0: String, arg1: number);
        constructor(arg0: InetAddress, arg1: number);
        constructor(arg0: String, arg1: number, arg2: InetAddress, arg3: number);
        constructor(arg0: InetAddress, arg1: number, arg2: InetAddress, arg3: number);
        constructor(arg0: String, arg1: number, arg2: boolean);
        constructor(arg0: InetAddress, arg1: number, arg2: boolean);

        connect(arg0: SocketAddress): void;

        connect(arg0: SocketAddress, arg1: number): void;

        bind(arg0: SocketAddress): void;

        getInetAddress(): InetAddress;

        getLocalAddress(): InetAddress;

        getPort(): number;

        getLocalPort(): number;

        getRemoteSocketAddress(): SocketAddress;

        getLocalSocketAddress(): SocketAddress;

        getChannel(): SocketChannel;

        getInputStream(): InputStream;

        getOutputStream(): OutputStream;

        setTcpNoDelay(arg0: boolean): void;

        getTcpNoDelay(): boolean;

        setSoLinger(arg0: boolean, arg1: number): void;

        getSoLinger(): number;

        sendUrgentData(arg0: number): void;

        setOOBInline(arg0: boolean): void;

        getOOBInline(): boolean;

        setSoTimeout(arg0: number): void;

        getSoTimeout(): number;

        setSendBufferSize(arg0: number): void;

        getSendBufferSize(): number;

        setReceiveBufferSize(arg0: number): void;

        getReceiveBufferSize(): number;

        setKeepAlive(arg0: boolean): void;

        getKeepAlive(): boolean;

        setTrafficClass(arg0: number): void;

        getTrafficClass(): number;

        setReuseAddress(arg0: boolean): void;

        getReuseAddress(): boolean;

        close(): void;

        shutdownInput(): void;

        shutdownOutput(): void;
        toString(): string;

        isConnected(): boolean;

        isBound(): boolean;

        isClosed(): boolean;

        isInputShutdown(): boolean;

        isOutputShutdown(): boolean;

        static setSocketImplFactory(arg0: SocketImplFactory): void;

        setPerformancePreferences(arg0: number, arg1: number, arg2: number): void;

        setOption<T extends Object>(arg0: SocketOption<T>, arg1: T): Socket;

        getOption<T extends Object>(arg0: SocketOption<T>): T;

        supportedOptions(): Set<SocketOption<any>>;
    }

    export abstract class SocketAddress implements Serializable {
        constructor();
    }

    export class SocketException extends IOException {
        constructor(arg0: String);
        constructor();
    }

    export abstract class SocketImpl implements SocketOptions {
        constructor();
        toString(): string;
    }

    export interface SocketImplFactory {

        createSocketImpl(): SocketImpl;
    }

    export interface SocketOption<T extends Object> extends Object {

        name(): String;

        type(): Class<T>;
    }

    export namespace SocketOptions {
        const TCP_NODELAY: number
        const SO_BINDADDR: number
        const SO_REUSEADDR: number
        const SO_REUSEPORT: number
        const SO_BROADCAST: number
        const IP_MULTICAST_IF: number
        const IP_MULTICAST_IF2: number
        const IP_MULTICAST_LOOP: number
        const IP_TOS: number
        const SO_LINGER: number
        const SO_TIMEOUT: number
        const SO_SNDBUF: number
        const SO_RCVBUF: number
        const SO_KEEPALIVE: number
        const SO_OOBINLINE: number
    }

    export interface SocketOptions {
        TCP_NODELAY: number
        SO_BINDADDR: number
        SO_REUSEADDR: number
        SO_REUSEPORT: number
        SO_BROADCAST: number
        IP_MULTICAST_IF: number
        IP_MULTICAST_IF2: number
        IP_MULTICAST_LOOP: number
        IP_TOS: number
        SO_LINGER: number
        SO_TIMEOUT: number
        SO_SNDBUF: number
        SO_RCVBUF: number
        SO_KEEPALIVE: number
        SO_OOBINLINE: number

        setOption(arg0: number, arg1: Object): void;

        getOption(arg0: number): Object;
    }

    export class SocketPermission extends Permission implements Serializable {
        constructor(arg0: String, arg1: String);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        newPermissionCollection(): PermissionCollection;
    }

    export class SocketTimeoutException extends InterruptedIOException {
        constructor(arg0: String);
        constructor();
    }

    export class StandardProtocolFamily extends Enum<StandardProtocolFamily> implements ProtocolFamily {
        static INET: StandardProtocolFamily
        static INET6: StandardProtocolFamily
        static UNIX: StandardProtocolFamily

        static values(): StandardProtocolFamily[];

        static valueOf(arg0: String): StandardProtocolFamily;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class StandardSocketOptions {
        static SO_BROADCAST: SocketOption<Boolean>
        static SO_KEEPALIVE: SocketOption<Boolean>
        static SO_SNDBUF: SocketOption<Number>
        static SO_RCVBUF: SocketOption<Number>
        static SO_REUSEADDR: SocketOption<Boolean>
        static SO_REUSEPORT: SocketOption<Boolean>
        static SO_LINGER: SocketOption<Number>
        static IP_TOS: SocketOption<Number>
        static IP_MULTICAST_IF: SocketOption<NetworkInterface>
        static IP_MULTICAST_TTL: SocketOption<Number>
        static IP_MULTICAST_LOOP: SocketOption<Boolean>
        static TCP_NODELAY: SocketOption<Boolean>
    }

    export class URI extends Object implements Comparable<URI>, Serializable {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String, arg2: String, arg3: number, arg4: String, arg5: String, arg6: String);
        constructor(arg0: String, arg1: String, arg2: String, arg3: String, arg4: String);
        constructor(arg0: String, arg1: String, arg2: String, arg3: String);
        constructor(arg0: String, arg1: String, arg2: String);

        static create(arg0: String): URI;

        parseServerAuthority(): URI;

        normalize(): URI;

        resolve(arg0: URI): URI;

        resolve(arg0: String): URI;

        relativize(arg0: URI): URI;

        toURL(): URL;

        getScheme(): String;

        isAbsolute(): boolean;

        isOpaque(): boolean;

        getRawSchemeSpecificPart(): String;

        getSchemeSpecificPart(): String;

        getRawAuthority(): String;

        getAuthority(): String;

        getRawUserInfo(): String;

        getUserInfo(): String;

        getHost(): String;

        getPort(): number;

        getRawPath(): String;

        getPath(): String;

        getRawQuery(): String;

        getQuery(): String;

        getRawFragment(): String;

        getFragment(): String;

        equals(arg0: Object): boolean;

        hashCode(): number;

        compareTo(arg0: URI): number;
        toString(): string;

        toASCIIString(): String;
    }

    export class URISyntaxException extends Exception {
        constructor(arg0: String, arg1: String, arg2: number);
        constructor(arg0: String, arg1: String);

        getInput(): String;

        getReason(): String;

        getIndex(): number;

        getMessage(): String;
    }

    export class URL implements Serializable {
        constructor(arg0: String, arg1: String, arg2: number, arg3: String);
        constructor(arg0: String, arg1: String, arg2: String);
        constructor(arg0: String, arg1: String, arg2: number, arg3: String, arg4: URLStreamHandler);
        constructor(arg0: String);
        constructor(arg0: URL, arg1: String);
        constructor(arg0: URL, arg1: String, arg2: URLStreamHandler);

        getQuery(): String;

        getPath(): String;

        getUserInfo(): String;

        getAuthority(): String;

        getPort(): number;

        getDefaultPort(): number;

        getProtocol(): String;

        getHost(): String;

        getFile(): String;

        getRef(): String;

        equals(arg0: Object): boolean;

        hashCode(): number;

        sameFile(arg0: URL): boolean;
        toString(): string;

        toExternalForm(): String;

        toURI(): URI;

        openConnection(): URLConnection;

        openConnection(arg0: Proxy): URLConnection;

        openStream(): InputStream;

        getContent(): Object;

        getContent(arg0: Class<any>[]): Object;

        static setURLStreamHandlerFactory(arg0: URLStreamHandlerFactory): void;
    }

    export class URLClassLoader extends SecureClassLoader implements Closeable {
        constructor(arg0: URL[], arg1: ClassLoader);
        constructor(arg0: URL[]);
        constructor(arg0: URL[], arg1: ClassLoader, arg2: URLStreamHandlerFactory);
        constructor(arg0: String, arg1: URL[], arg2: ClassLoader);
        constructor(arg0: String, arg1: URL[], arg2: ClassLoader, arg3: URLStreamHandlerFactory);

        getResourceAsStream(arg0: String): InputStream;

        close(): void;

        getURLs(): URL[];

        findResource(arg0: String): URL;

        findResources(arg0: String): Enumeration<URL>;

        static newInstance(arg0: URL[], arg1: ClassLoader): URLClassLoader;

        static newInstance(arg0: URL[]): URLClassLoader;
    }

    export abstract class URLConnection {

        static getFileNameMap(): FileNameMap;

        static setFileNameMap(arg0: FileNameMap): void;

        abstract connect(): void;

        setConnectTimeout(arg0: number): void;

        getConnectTimeout(): number;

        setReadTimeout(arg0: number): void;

        getReadTimeout(): number;

        getURL(): URL;

        getContentLength(): number;

        getContentLengthLong(): number;

        getContentType(): String;

        getContentEncoding(): String;

        getExpiration(): number;

        getDate(): number;

        getLastModified(): number;

        getHeaderField(arg0: String): String;

        getHeaderFields(): Map<String, List<String>>;

        getHeaderFieldInt(arg0: String, arg1: number): number;

        getHeaderFieldLong(arg0: String, arg1: number): number;

        getHeaderFieldDate(arg0: String, arg1: number): number;

        getHeaderFieldKey(arg0: number): String;

        getHeaderField(arg0: number): String;

        getContent(): Object;

        getContent(arg0: Class<any>[]): Object;

        getPermission(): Permission;

        getInputStream(): InputStream;

        getOutputStream(): OutputStream;
        toString(): string;

        setDoInput(arg0: boolean): void;

        getDoInput(): boolean;

        setDoOutput(arg0: boolean): void;

        getDoOutput(): boolean;

        setAllowUserInteraction(arg0: boolean): void;

        getAllowUserInteraction(): boolean;

        static setDefaultAllowUserInteraction(arg0: boolean): void;

        static getDefaultAllowUserInteraction(): boolean;

        setUseCaches(arg0: boolean): void;

        getUseCaches(): boolean;

        setIfModifiedSince(arg0: number): void;

        getIfModifiedSince(): number;

        getDefaultUseCaches(): boolean;

        setDefaultUseCaches(arg0: boolean): void;

        static setDefaultUseCaches(arg0: String, arg1: boolean): void;

        static getDefaultUseCaches(arg0: String): boolean;

        setRequestProperty(arg0: String, arg1: String): void;

        addRequestProperty(arg0: String, arg1: String): void;

        getRequestProperty(arg0: String): String;

        getRequestProperties(): Map<String, List<String>>;

        static setDefaultRequestProperty(arg0: String, arg1: String): void;

        static getDefaultRequestProperty(arg0: String): String;

        static setContentHandlerFactory(arg0: ContentHandlerFactory): void;

        static guessContentTypeFromName(arg0: String): String;

        static guessContentTypeFromStream(arg0: InputStream): String;
    }

    export class URLDecoder {

        static decode(arg0: String): String;

        static decode(arg0: String, arg1: String): String;

        static decode(arg0: String, arg1: Charset): String;
    }

    export class URLEncoder {

        static encode(arg0: String): String;

        static encode(arg0: String, arg1: String): String;

        static encode(arg0: String, arg1: Charset): String;
    }

    export class URLPermission extends Permission {
        constructor(arg0: String, arg1: String);
        constructor(arg0: String);

        getActions(): String;

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export abstract class URLStreamHandler {
        constructor();
    }

    export interface URLStreamHandlerFactory {

        createURLStreamHandler(arg0: String): URLStreamHandler;
    }

    export class UnixDomainSocketAddress extends SocketAddress {

        static of(arg0: String): UnixDomainSocketAddress;

        static of(arg0: Path): UnixDomainSocketAddress;

        getPath(): Path;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

    export class UnknownHostException extends IOException {
        constructor(arg0: String);
        constructor();
    }

    export class UnknownServiceException extends IOException {
        constructor();
        constructor(arg0: String);
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
declare module '@java/java.math' {
    import { Enum, Comparable, Class, String, Number } from '@java/java.lang'
    import { Random } from '@java/java.util'
    import { Serializable } from '@java/java.io'
    export class BigDecimal extends Number implements Comparable<BigDecimal> {
        static ZERO: BigDecimal
        static ONE: BigDecimal
        static TEN: BigDecimal
        static ROUND_UP: number
        static ROUND_DOWN: number
        static ROUND_CEILING: number
        static ROUND_FLOOR: number
        static ROUND_HALF_UP: number
        static ROUND_HALF_DOWN: number
        static ROUND_HALF_EVEN: number
        static ROUND_UNNECESSARY: number
        constructor(arg0: String[], arg1: number, arg2: number);
        constructor(arg0: String[], arg1: number, arg2: number, arg3: MathContext);
        constructor(arg0: String[]);
        constructor(arg0: String[], arg1: MathContext);
        constructor(arg0: String);
        constructor(arg0: String, arg1: MathContext);
        constructor(arg0: number);
        constructor(arg0: number, arg1: MathContext);
        constructor(arg0: BigInteger);
        constructor(arg0: BigInteger, arg1: MathContext);
        constructor(arg0: BigInteger, arg1: number);
        constructor(arg0: BigInteger, arg1: number, arg2: MathContext);
        constructor(arg0: number);
        constructor(arg0: number, arg1: MathContext);
        constructor(arg0: number);
        constructor(arg0: number, arg1: MathContext);

        static valueOf(arg0: number, arg1: number): BigDecimal;

        static valueOf(arg0: number): BigDecimal;

        static valueOf(arg0: number): BigDecimal;

        add(arg0: BigDecimal): BigDecimal;

        add(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        subtract(arg0: BigDecimal): BigDecimal;

        subtract(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        multiply(arg0: BigDecimal): BigDecimal;

        multiply(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        divide(arg0: BigDecimal, arg1: number, arg2: number): BigDecimal;

        divide(arg0: BigDecimal, arg1: number, arg2: RoundingMode): BigDecimal;

        divide(arg0: BigDecimal, arg1: number): BigDecimal;

        divide(arg0: BigDecimal, arg1: RoundingMode): BigDecimal;

        divide(arg0: BigDecimal): BigDecimal;

        divide(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        divideToIntegralValue(arg0: BigDecimal): BigDecimal;

        divideToIntegralValue(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        remainder(arg0: BigDecimal): BigDecimal;

        remainder(arg0: BigDecimal, arg1: MathContext): BigDecimal;

        divideAndRemainder(arg0: BigDecimal): BigDecimal[];

        divideAndRemainder(arg0: BigDecimal, arg1: MathContext): BigDecimal[];

        sqrt(arg0: MathContext): BigDecimal;

        pow(arg0: number): BigDecimal;

        pow(arg0: number, arg1: MathContext): BigDecimal;

        abs(): BigDecimal;

        abs(arg0: MathContext): BigDecimal;

        negate(): BigDecimal;

        negate(arg0: MathContext): BigDecimal;

        plus(): BigDecimal;

        plus(arg0: MathContext): BigDecimal;

        signum(): number;

        scale(): number;

        precision(): number;

        unscaledValue(): BigInteger;

        round(arg0: MathContext): BigDecimal;

        setScale(arg0: number, arg1: RoundingMode): BigDecimal;

        setScale(arg0: number, arg1: number): BigDecimal;

        setScale(arg0: number): BigDecimal;

        movePointLeft(arg0: number): BigDecimal;

        movePointRight(arg0: number): BigDecimal;

        scaleByPowerOfTen(arg0: number): BigDecimal;

        stripTrailingZeros(): BigDecimal;

        compareTo(arg0: BigDecimal): number;

        equals(arg0: Object): boolean;

        min(arg0: BigDecimal): BigDecimal;

        max(arg0: BigDecimal): BigDecimal;

        hashCode(): number;
        toString(): string;

        toEngineeringString(): String;

        toPlainString(): String;

        toBigInteger(): BigInteger;

        toBigIntegerExact(): BigInteger;

        longValue(): number;

        longValueExact(): number;

        intValue(): number;

        intValueExact(): number;

        shortValueExact(): number;

        byteValueExact(): number;

        floatValue(): number;

        doubleValue(): number;

        ulp(): BigDecimal;
    }

    export class BigInteger extends Number implements Comparable<BigInteger> {
        static ZERO: BigInteger
        static ONE: BigInteger
        static TWO: BigInteger
        static TEN: BigInteger
        constructor(arg0: number[], arg1: number, arg2: number);
        constructor(arg0: number[]);
        constructor(arg0: number, arg1: number[], arg2: number, arg3: number);
        constructor(arg0: number, arg1: number[]);
        constructor(arg0: String, arg1: number);
        constructor(arg0: String);
        constructor(arg0: number, arg1: Random);
        constructor(arg0: number, arg1: number, arg2: Random);

        static probablePrime(arg0: number, arg1: Random): BigInteger;

        nextProbablePrime(): BigInteger;

        static valueOf(arg0: number): BigInteger;

        add(arg0: BigInteger): BigInteger;

        subtract(arg0: BigInteger): BigInteger;

        multiply(arg0: BigInteger): BigInteger;

        divide(arg0: BigInteger): BigInteger;

        divideAndRemainder(arg0: BigInteger): BigInteger[];

        remainder(arg0: BigInteger): BigInteger;

        pow(arg0: number): BigInteger;

        sqrt(): BigInteger;

        sqrtAndRemainder(): BigInteger[];

        gcd(arg0: BigInteger): BigInteger;

        abs(): BigInteger;

        negate(): BigInteger;

        signum(): number;

        mod(arg0: BigInteger): BigInteger;

        modPow(arg0: BigInteger, arg1: BigInteger): BigInteger;

        modInverse(arg0: BigInteger): BigInteger;

        shiftLeft(arg0: number): BigInteger;

        shiftRight(arg0: number): BigInteger;

        and(arg0: BigInteger): BigInteger;

        or(arg0: BigInteger): BigInteger;

        xor(arg0: BigInteger): BigInteger;

        not(): BigInteger;

        andNot(arg0: BigInteger): BigInteger;

        testBit(arg0: number): boolean;

        setBit(arg0: number): BigInteger;

        clearBit(arg0: number): BigInteger;

        flipBit(arg0: number): BigInteger;

        getLowestSetBit(): number;

        bitLength(): number;

        bitCount(): number;

        isProbablePrime(arg0: number): boolean;

        compareTo(arg0: BigInteger): number;

        equals(arg0: Object): boolean;

        min(arg0: BigInteger): BigInteger;

        max(arg0: BigInteger): BigInteger;

        hashCode(): number;

        toString(arg0: number): String;
        toString(): string;

        toByteArray(): number[];

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;

        longValueExact(): number;

        intValueExact(): number;

        shortValueExact(): number;

        byteValueExact(): number;
    }

    export class MathContext implements Serializable {
        static UNLIMITED: MathContext
        static DECIMAL32: MathContext
        static DECIMAL64: MathContext
        static DECIMAL128: MathContext
        constructor(arg0: number);
        constructor(arg0: number, arg1: RoundingMode);
        constructor(arg0: String);

        getPrecision(): number;

        getRoundingMode(): RoundingMode;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export class RoundingMode extends Enum<RoundingMode> {
        static UP: RoundingMode
        static DOWN: RoundingMode
        static CEILING: RoundingMode
        static FLOOR: RoundingMode
        static HALF_UP: RoundingMode
        static HALF_DOWN: RoundingMode
        static HALF_EVEN: RoundingMode
        static UNNECESSARY: RoundingMode

        static values(): RoundingMode[];

        static valueOf(arg0: String): RoundingMode;

        static valueOf(arg0: number): RoundingMode;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.lang.invoke.d.ts" />
declare module '@java/java.lang.runtime' {
    import { Class, String } from '@java/java.lang'
    import { MethodType, Lookup, CallSite, TypeDescriptor, MethodHandle } from '@java/java.lang.invoke'
    export class ObjectMethods {

        static bootstrap(arg0: Lookup, arg1: String, arg2: TypeDescriptor, arg3: Class<any>, arg4: String, arg5: MethodHandle[]): Object;
    }

    export class SwitchBootstraps {

        static typeSwitch(arg0: Lookup, arg1: String, arg2: MethodType, arg3: Object[]): CallSite;

        static enumSwitch(arg0: Lookup, arg1: String, arg2: MethodType, arg3: Object[]): CallSite;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.lang.annotation.d.ts" />
declare module '@java/java.lang.reflect' {
    import { BasicPermission } from '@java/java.security'
    import { ClassFormatError, RuntimeException, ReflectiveOperationException, Throwable, ClassLoader, Class, String } from '@java/java.lang'
    import { Serializable } from '@java/java.io'
    import { Annotation } from '@java/java.lang.annotation'
    export class AccessibleObject implements AnnotatedElement {

        static setAccessible(arg0: AccessibleObject[], arg1: boolean): void;

        setAccessible(arg0: boolean): void;

        trySetAccessible(): boolean;

        isAccessible(): boolean;

        canAccess(arg0: Object): boolean;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        isAnnotationPresent(arg0: Class<Annotation>): boolean;

        getAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getAnnotations(): Annotation[];

        getDeclaredAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getDeclaredAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getDeclaredAnnotations(): Annotation[];
    }

    export interface AnnotatedArrayType extends AnnotatedType {

        getAnnotatedGenericComponentType(): AnnotatedType;

        getAnnotatedOwnerType(): AnnotatedType;
    }

    export interface AnnotatedElement {

/* default */ isAnnotationPresent(arg0: Class<Annotation>): boolean;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotations(): Annotation[];

/* default */ getAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

/* default */ getDeclaredAnnotation<T extends Annotation>(arg0: Class<T>): T;

/* default */ getDeclaredAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getDeclaredAnnotations(): Annotation[];
    }

    export interface AnnotatedParameterizedType extends AnnotatedType {

        getAnnotatedActualTypeArguments(): AnnotatedType[];

        getAnnotatedOwnerType(): AnnotatedType;
    }

    export interface AnnotatedType extends AnnotatedElement {

/* default */ getAnnotatedOwnerType(): AnnotatedType;

        getType(): Type;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotations(): Annotation[];

        getDeclaredAnnotations(): Annotation[];
    }

    export interface AnnotatedTypeVariable extends AnnotatedType {

        getAnnotatedBounds(): AnnotatedType[];

        getAnnotatedOwnerType(): AnnotatedType;
    }

    export interface AnnotatedWildcardType extends AnnotatedType {

        getAnnotatedLowerBounds(): AnnotatedType[];

        getAnnotatedUpperBounds(): AnnotatedType[];

        getAnnotatedOwnerType(): AnnotatedType;
    }

    export class Array {

        static newInstance(arg0: Class<any>, arg1: number): Object;

        static newInstance(arg0: Class<any>, arg1: number[]): Object;

        static getLength(arg0: Object): number;

        static get(arg0: Object, arg1: number): Object;

        static getBoolean(arg0: Object, arg1: number): boolean;

        static getByte(arg0: Object, arg1: number): number;

        static getChar(arg0: Object, arg1: number): String;

        static getShort(arg0: Object, arg1: number): number;

        static getInt(arg0: Object, arg1: number): number;

        static getLong(arg0: Object, arg1: number): number;

        static getFloat(arg0: Object, arg1: number): number;

        static getDouble(arg0: Object, arg1: number): number;

        static set(arg0: Object, arg1: number, arg2: Object): void;

        static setBoolean(arg0: Object, arg1: number, arg2: boolean): void;

        static setByte(arg0: Object, arg1: number, arg2: number): void;

        static setChar(arg0: Object, arg1: number, arg2: String): void;

        static setShort(arg0: Object, arg1: number, arg2: number): void;

        static setInt(arg0: Object, arg1: number, arg2: number): void;

        static setLong(arg0: Object, arg1: number, arg2: number): void;

        static setFloat(arg0: Object, arg1: number, arg2: number): void;

        static setDouble(arg0: Object, arg1: number, arg2: number): void;
    }

    export interface Constructor<T extends Object> { }
    export class Constructor<T extends Object> extends Executable {

        setAccessible(arg0: boolean): void;

        getDeclaringClass(): Class<T>;

        getName(): String;

        getModifiers(): number;

        getTypeParameters(): TypeVariable<Constructor<T>>[];

        getParameterTypes(): Class<any>[];

        getParameterCount(): number;

        getGenericParameterTypes(): Type[];

        getExceptionTypes(): Class<any>[];

        getGenericExceptionTypes(): Type[];

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        toGenericString(): String;

        newInstance(arg0: Object[]): T;

        isVarArgs(): boolean;

        isSynthetic(): boolean;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getDeclaredAnnotations(): Annotation[];

        getParameterAnnotations(): Array<Array<Annotation>>;

        getAnnotatedReturnType(): AnnotatedType;

        getAnnotatedReceiverType(): AnnotatedType;
    }

    export interface Executable extends Member, GenericDeclaration { }
    export abstract class Executable extends AccessibleObject implements Member, GenericDeclaration {

        abstract getDeclaringClass(): Class<any>;

        abstract getName(): String;

        abstract getModifiers(): number;

        abstract getTypeParameters(): TypeVariable<any>[];

        abstract getParameterTypes(): Class<any>[];

        getParameterCount(): number;

        getGenericParameterTypes(): Type[];

        getParameters(): Parameter[];

        abstract getExceptionTypes(): Class<any>[];

        getGenericExceptionTypes(): Type[];

        abstract toGenericString(): String;

        isVarArgs(): boolean;

        isSynthetic(): boolean;

        abstract getParameterAnnotations(): Array<Array<Annotation>>;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getDeclaredAnnotations(): Annotation[];

        abstract getAnnotatedReturnType(): AnnotatedType;

        getAnnotatedReceiverType(): AnnotatedType;

        getAnnotatedParameterTypes(): AnnotatedType[];

        getAnnotatedExceptionTypes(): AnnotatedType[];
    }

    export interface Field extends Member { }
    export class Field extends AccessibleObject implements Member {

        setAccessible(arg0: boolean): void;

        getDeclaringClass(): Class<any>;

        getName(): String;

        getModifiers(): number;

        isEnumConstant(): boolean;

        isSynthetic(): boolean;

        getType(): Class<any>;

        getGenericType(): Type;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        toGenericString(): String;

        get(arg0: Object): Object;

        getBoolean(arg0: Object): boolean;

        getByte(arg0: Object): number;

        getChar(arg0: Object): String;

        getShort(arg0: Object): number;

        getInt(arg0: Object): number;

        getLong(arg0: Object): number;

        getFloat(arg0: Object): number;

        getDouble(arg0: Object): number;

        set(arg0: Object, arg1: Object): void;

        setBoolean(arg0: Object, arg1: boolean): void;

        setByte(arg0: Object, arg1: number): void;

        setChar(arg0: Object, arg1: String): void;

        setShort(arg0: Object, arg1: number): void;

        setInt(arg0: Object, arg1: number): void;

        setLong(arg0: Object, arg1: number): void;

        setFloat(arg0: Object, arg1: number): void;

        setDouble(arg0: Object, arg1: number): void;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getDeclaredAnnotations(): Annotation[];

        getAnnotatedType(): AnnotatedType;
    }

    export interface GenericArrayType extends Type {

        getGenericComponentType(): Type;
    }

    export interface GenericDeclaration extends AnnotatedElement {

        getTypeParameters(): TypeVariable<any>[];
    }

    export class GenericSignatureFormatError extends ClassFormatError {
        constructor();
        constructor(arg0: String);
    }

    export class InaccessibleObjectException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export namespace InvocationHandler {
        function
/* default */ invokeDefault(arg0: Object, arg1: Method, arg2: Object[]): Object;
    }

    export interface InvocationHandler {

        invoke(arg0: Object, arg1: Method, arg2: Object[]): Object;
    }

    export class InvocationTargetException extends ReflectiveOperationException {
        constructor(arg0: Throwable);
        constructor(arg0: Throwable, arg1: String);

        getTargetException(): Throwable;

        getCause(): Throwable;
    }

    export class MalformedParameterizedTypeException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class MalformedParametersException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export namespace Member {
        const PUBLIC: number
        const DECLARED: number
    }

    export interface Member {
        PUBLIC: number
        DECLARED: number

        getDeclaringClass(): Class<any>;

        getName(): String;

        getModifiers(): number;

        isSynthetic(): boolean;
    }

    export interface Method { }
    export class Method extends Executable {

        setAccessible(arg0: boolean): void;

        getDeclaringClass(): Class<any>;

        getName(): String;

        getModifiers(): number;

        getTypeParameters(): TypeVariable<Method>[];

        getReturnType(): Class<any>;

        getGenericReturnType(): Type;

        getParameterTypes(): Class<any>[];

        getParameterCount(): number;

        getGenericParameterTypes(): Type[];

        getExceptionTypes(): Class<any>[];

        getGenericExceptionTypes(): Type[];

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        toGenericString(): String;

        invoke(arg0: Object, arg1: Object[]): Object;

        isBridge(): boolean;

        isVarArgs(): boolean;

        isSynthetic(): boolean;

        isDefault(): boolean;

        getDefaultValue(): Object;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getDeclaredAnnotations(): Annotation[];

        getParameterAnnotations(): Array<Array<Annotation>>;

        getAnnotatedReturnType(): AnnotatedType;
    }

    export class Modifier {
        static PUBLIC: number
        static PRIVATE: number
        static PROTECTED: number
        static STATIC: number
        static FINAL: number
        static SYNCHRONIZED: number
        static VOLATILE: number
        static TRANSIENT: number
        static NATIVE: number
        static INTERFACE: number
        static ABSTRACT: number
        static STRICT: number

        static isPublic(arg0: number): boolean;

        static isPrivate(arg0: number): boolean;

        static isProtected(arg0: number): boolean;

        static isStatic(arg0: number): boolean;

        static isFinal(arg0: number): boolean;

        static isSynchronized(arg0: number): boolean;

        static isVolatile(arg0: number): boolean;

        static isTransient(arg0: number): boolean;

        static isNative(arg0: number): boolean;

        static isInterface(arg0: number): boolean;

        static isAbstract(arg0: number): boolean;

        static isStrict(arg0: number): boolean;

        static toString(arg0: number): String;

        static classModifiers(): number;

        static interfaceModifiers(): number;

        static constructorModifiers(): number;

        static methodModifiers(): number;

        static fieldModifiers(): number;

        static parameterModifiers(): number;
    }

    export interface Parameter extends AnnotatedElement { }
    export class Parameter implements AnnotatedElement {

        equals(arg0: Object): boolean;

        hashCode(): number;

        isNamePresent(): boolean;
        toString(): string;

        getDeclaringExecutable(): Executable;

        getModifiers(): number;

        getName(): String;

        getParameterizedType(): Type;

        getType(): Class<any>;

        getAnnotatedType(): AnnotatedType;

        isImplicit(): boolean;

        isSynthetic(): boolean;

        isVarArgs(): boolean;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getDeclaredAnnotations(): Annotation[];

        getDeclaredAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getDeclaredAnnotationsByType<T extends Annotation>(arg0: Class<T>): T[];

        getAnnotations(): Annotation[];
    }

    export interface ParameterizedType extends Type {

        getActualTypeArguments(): Type[];

        getRawType(): Type;

        getOwnerType(): Type;
    }

    export class Proxy implements Serializable {

        static getProxyClass(arg0: ClassLoader, arg1: Class<any>[]): Class<any>;

        static newProxyInstance(arg0: ClassLoader, arg1: Class<any>[], arg2: InvocationHandler): Object;

        static isProxyClass(arg0: Class<any>): boolean;

        static getInvocationHandler(arg0: Object): InvocationHandler;
    }

    export interface RecordComponent extends AnnotatedElement { }
    export class RecordComponent implements AnnotatedElement {

        getName(): String;

        getType(): Class<any>;

        getGenericSignature(): String;

        getGenericType(): Type;

        getAnnotatedType(): AnnotatedType;

        getAccessor(): Method;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotations(): Annotation[];

        getDeclaredAnnotations(): Annotation[];
        toString(): string;

        getDeclaringRecord(): Class<any>;
    }

    export class ReflectPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export interface Type {

/* default */ getTypeName(): String;
    }

    export interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement, Object {

        getBounds(): Type[];

        getGenericDeclaration(): D;

        getName(): String;

        getAnnotatedBounds(): AnnotatedType[];
    }

    export class UndeclaredThrowableException extends RuntimeException {
        constructor(arg0: Throwable);
        constructor(arg0: Throwable, arg1: String);

        getUndeclaredThrowable(): Throwable;
    }

    export interface WildcardType extends Type {

        getUpperBounds(): Type[];

        getLowerBounds(): Type[];
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
declare module '@java/java.lang.ref' {
    import { Runnable } from '@java/java.lang'
    import { ThreadFactory } from '@java/java.util.concurrent'
    export class Cleaner {

        static create(): Cleaner;

        static create(arg0: ThreadFactory): Cleaner;

        register(arg0: Object, arg1: Runnable): Cleaner.Cleanable;
    }
    export namespace Cleaner {
        export interface Cleanable {

            clean(): void;
        }

    }

    export class PhantomReference<T extends Object> extends Reference<T> {
        constructor(arg0: T, arg1: ReferenceQueue<T>);

        get(): T;
    }

    export abstract class Reference<T extends Object> extends Object {

        get(): T;

        refersTo(arg0: T): boolean;

        clear(): void;

        isEnqueued(): boolean;

        enqueue(): boolean;

        static reachabilityFence(arg0: Object): void;
    }

    export class ReferenceQueue<T extends Object> extends Object {
        constructor();

        poll(): Reference<T>;

        remove(arg0: number): Reference<T>;

        remove(): Reference<T>;
    }

    export class SoftReference<T extends Object> extends Reference<T> {
        constructor(arg0: T);
        constructor(arg0: T, arg1: ReferenceQueue<T>);

        get(): T;
    }

    export class WeakReference<T extends Object> extends Reference<T> {
        constructor(arg0: T);
        constructor(arg0: T, arg1: ReferenceQueue<T>);
    }

}
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.nio.file.d.ts" />
declare module '@java/java.lang.module' {
    import { Collection, List, Set, Optional } from '@java/java.util'
    import { Enum, Throwable, Comparable, Class, String, RuntimeException } from '@java/java.lang'
    import { URI } from '@java/java.net'
    import { InputStream, Closeable } from '@java/java.io'
    import { Stream } from '@java/java.util.stream'
    import { ByteBuffer } from '@java/java.nio'
    import { Supplier } from '@java/java.util.function'
    import { Path } from '@java/java.nio.file'
    export class Configuration {

        resolve(arg0: ModuleFinder, arg1: ModuleFinder, arg2: Collection<String>): Configuration;

        resolveAndBind(arg0: ModuleFinder, arg1: ModuleFinder, arg2: Collection<String>): Configuration;

        static resolve(arg0: ModuleFinder, arg1: List<Configuration>, arg2: ModuleFinder, arg3: Collection<String>): Configuration;

        static resolveAndBind(arg0: ModuleFinder, arg1: List<Configuration>, arg2: ModuleFinder, arg3: Collection<String>): Configuration;

        static empty(): Configuration;

        parents(): List<Configuration>;

        modules(): Set<ResolvedModule>;

        findModule(arg0: String): Optional<ResolvedModule>;
        toString(): string;
    }

    export class FindException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export class InvalidModuleDescriptorException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class ModuleDescriptor extends Object implements Comparable<ModuleDescriptor> {

        name(): String;

        modifiers(): Set<ModuleDescriptor.Modifier>;

        isOpen(): boolean;

        isAutomatic(): boolean;

        requires(): Set<ModuleDescriptor.Requires>;

        exports(): Set<ModuleDescriptor.Exports>;

        opens(): Set<ModuleDescriptor.Opens>;

        uses(): Set<String>;

        provides(): Set<ModuleDescriptor.Provides>;

        version(): Optional<ModuleDescriptor.Version>;

        rawVersion(): Optional<String>;

        toNameAndVersion(): String;

        mainClass(): Optional<String>;

        packages(): Set<String>;

        compareTo(arg0: ModuleDescriptor): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        static newModule(arg0: String, arg1: Set<ModuleDescriptor.Modifier>): ModuleDescriptor.Builder;

        static newModule(arg0: String): ModuleDescriptor.Builder;

        static newOpenModule(arg0: String): ModuleDescriptor.Builder;

        static newAutomaticModule(arg0: String): ModuleDescriptor.Builder;

        static read(arg0: InputStream, arg1: Supplier<Set<String>>): ModuleDescriptor;

        static read(arg0: InputStream): ModuleDescriptor;

        static read(arg0: ByteBuffer, arg1: Supplier<Set<String>>): ModuleDescriptor;

        static read(arg0: ByteBuffer): ModuleDescriptor;
    }
    export namespace ModuleDescriptor {
        export class Builder {

            requires(arg0: ModuleDescriptor.Requires): ModuleDescriptor.Builder;

            requires(arg0: Set<ModuleDescriptor.Requires.Modifier>, arg1: String, arg2: ModuleDescriptor.Version): ModuleDescriptor.Builder;

            requires(arg0: Set<ModuleDescriptor.Requires.Modifier>, arg1: String): ModuleDescriptor.Builder;

            requires(arg0: String): ModuleDescriptor.Builder;

            exports(arg0: ModuleDescriptor.Exports): ModuleDescriptor.Builder;

            exports(arg0: Set<ModuleDescriptor.Exports.Modifier>, arg1: String, arg2: Set<String>): ModuleDescriptor.Builder;

            exports(arg0: Set<ModuleDescriptor.Exports.Modifier>, arg1: String): ModuleDescriptor.Builder;

            exports(arg0: String, arg1: Set<String>): ModuleDescriptor.Builder;

            exports(arg0: String): ModuleDescriptor.Builder;

            opens(arg0: ModuleDescriptor.Opens): ModuleDescriptor.Builder;

            opens(arg0: Set<ModuleDescriptor.Opens.Modifier>, arg1: String, arg2: Set<String>): ModuleDescriptor.Builder;

            opens(arg0: Set<ModuleDescriptor.Opens.Modifier>, arg1: String): ModuleDescriptor.Builder;

            opens(arg0: String, arg1: Set<String>): ModuleDescriptor.Builder;

            opens(arg0: String): ModuleDescriptor.Builder;

            uses(arg0: String): ModuleDescriptor.Builder;

            provides(arg0: ModuleDescriptor.Provides): ModuleDescriptor.Builder;

            provides(arg0: String, arg1: List<String>): ModuleDescriptor.Builder;

            packages(arg0: Set<String>): ModuleDescriptor.Builder;

            version(arg0: ModuleDescriptor.Version): ModuleDescriptor.Builder;

            version(arg0: String): ModuleDescriptor.Builder;

            mainClass(arg0: String): ModuleDescriptor.Builder;

            build(): ModuleDescriptor;
        }

        export class Exports extends Object implements Comparable<ModuleDescriptor.Exports> {

            modifiers(): Set<ModuleDescriptor.Exports.Modifier>;

            isQualified(): boolean;

            source(): String;

            targets(): Set<String>;

            compareTo(arg0: ModuleDescriptor.Exports): number;

            hashCode(): number;

            equals(arg0: Object): boolean;
            toString(): string;
        }
        export namespace Exports {
            export class Modifier extends Enum<ModuleDescriptor.Exports.Modifier> {
                static SYNTHETIC: ModuleDescriptor.Exports.Modifier
                static MANDATED: ModuleDescriptor.Exports.Modifier

                static values(): ModuleDescriptor.Exports.Modifier[];

                static valueOf(arg0: String): ModuleDescriptor.Exports.Modifier;
                /**
                * DO NOT USE
                */
                static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
            }

        }

        export class Modifier extends Enum<ModuleDescriptor.Exports.Modifier> {
            static SYNTHETIC: ModuleDescriptor.Exports.Modifier
            static MANDATED: ModuleDescriptor.Exports.Modifier

            static values(): ModuleDescriptor.Exports.Modifier[];

            static valueOf(arg0: String): ModuleDescriptor.Exports.Modifier;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Modifier extends Enum<ModuleDescriptor.Modifier> {
            static OPEN: ModuleDescriptor.Modifier
            static AUTOMATIC: ModuleDescriptor.Modifier
            static SYNTHETIC: ModuleDescriptor.Modifier
            static MANDATED: ModuleDescriptor.Modifier

            static values(): ModuleDescriptor.Modifier[];

            static valueOf(arg0: String): ModuleDescriptor.Modifier;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Opens extends Object implements Comparable<ModuleDescriptor.Opens> {

            modifiers(): Set<ModuleDescriptor.Opens.Modifier>;

            isQualified(): boolean;

            source(): String;

            targets(): Set<String>;

            compareTo(arg0: ModuleDescriptor.Opens): number;

            hashCode(): number;

            equals(arg0: Object): boolean;
            toString(): string;
        }
        export namespace Opens {
            export class Modifier extends Enum<ModuleDescriptor.Opens.Modifier> {
                static SYNTHETIC: ModuleDescriptor.Opens.Modifier
                static MANDATED: ModuleDescriptor.Opens.Modifier

                static values(): ModuleDescriptor.Opens.Modifier[];

                static valueOf(arg0: String): ModuleDescriptor.Opens.Modifier;
                /**
                * DO NOT USE
                */
                static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
            }

        }

        export class Modifier extends Enum<ModuleDescriptor.Opens.Modifier> {
            static SYNTHETIC: ModuleDescriptor.Opens.Modifier
            static MANDATED: ModuleDescriptor.Opens.Modifier

            static values(): ModuleDescriptor.Opens.Modifier[];

            static valueOf(arg0: String): ModuleDescriptor.Opens.Modifier;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Provides extends Object implements Comparable<ModuleDescriptor.Provides> {

            service(): String;

            providers(): List<String>;

            compareTo(arg0: ModuleDescriptor.Provides): number;

            hashCode(): number;

            equals(arg0: Object): boolean;
            toString(): string;
        }

        export class Requires extends Object implements Comparable<ModuleDescriptor.Requires> {

            modifiers(): Set<ModuleDescriptor.Requires.Modifier>;

            name(): String;

            compiledVersion(): Optional<ModuleDescriptor.Version>;

            rawCompiledVersion(): Optional<String>;

            compareTo(arg0: ModuleDescriptor.Requires): number;

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }
        export namespace Requires {
            export class Modifier extends Enum<ModuleDescriptor.Requires.Modifier> {
                static TRANSITIVE: ModuleDescriptor.Requires.Modifier
                static STATIC: ModuleDescriptor.Requires.Modifier
                static SYNTHETIC: ModuleDescriptor.Requires.Modifier
                static MANDATED: ModuleDescriptor.Requires.Modifier

                static values(): ModuleDescriptor.Requires.Modifier[];

                static valueOf(arg0: String): ModuleDescriptor.Requires.Modifier;
                /**
                * DO NOT USE
                */
                static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
            }

        }

        export class Modifier extends Enum<ModuleDescriptor.Requires.Modifier> {
            static TRANSITIVE: ModuleDescriptor.Requires.Modifier
            static STATIC: ModuleDescriptor.Requires.Modifier
            static SYNTHETIC: ModuleDescriptor.Requires.Modifier
            static MANDATED: ModuleDescriptor.Requires.Modifier

            static values(): ModuleDescriptor.Requires.Modifier[];

            static valueOf(arg0: String): ModuleDescriptor.Requires.Modifier;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class Version extends Object implements Comparable<ModuleDescriptor.Version> {

            static parse(arg0: String): ModuleDescriptor.Version;

            compareTo(arg0: ModuleDescriptor.Version): number;

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

    }

    export namespace ModuleFinder {
        function
/* default */ ofSystem(): ModuleFinder;
        function
/* default */ of(arg0: Path[]): ModuleFinder;
        function
/* default */ compose(arg0: ModuleFinder[]): ModuleFinder;
    }

    export interface ModuleFinder {

        find(arg0: String): Optional<ModuleReference>;

        findAll(): Set<ModuleReference>;
    }

    export interface ModuleReader extends Closeable {

        find(arg0: String): Optional<URI>;

/* default */ open(arg0: String): Optional<InputStream>;

/* default */ read(arg0: String): Optional<ByteBuffer>;

/* default */ release(arg0: ByteBuffer): void;

        list(): Stream<String>;

        close(): void;
    }

    export abstract class ModuleReference {

        descriptor(): ModuleDescriptor;

        location(): Optional<URI>;

        abstract open(): ModuleReader;
    }

    export class ResolutionException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export class ResolvedModule {

        configuration(): Configuration;

        reference(): ModuleReference;

        name(): String;

        reads(): Set<ResolvedModule>;

        hashCode(): number;

        equals(arg0: Object): boolean;
        toString(): string;
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="javax.management.d.ts" />
/// <reference path="javax.management.openmbean.d.ts" />
declare module '@java/java.lang.management' {
    import { BasicPermission } from '@java/java.security'
    import { Enum, StackTraceElement, Class, String, Thread } from '@java/java.lang'
    import { Map, List, Set } from '@java/java.util'
    import { MBeanServerConnection, MBeanServer, ObjectName } from '@java/javax.management'
    import { CompositeData } from '@java/javax.management.openmbean'
    export interface BufferPoolMXBean extends PlatformManagedObject {

        getName(): String;

        getCount(): number;

        getTotalCapacity(): number;

        getMemoryUsed(): number;
    }

    export interface ClassLoadingMXBean extends PlatformManagedObject {

        getTotalLoadedClassCount(): number;

        getLoadedClassCount(): number;

        getUnloadedClassCount(): number;

        isVerbose(): boolean;

        setVerbose(arg0: boolean): void;
    }

    export interface CompilationMXBean extends PlatformManagedObject {

        getName(): String;

        isCompilationTimeMonitoringSupported(): boolean;

        getTotalCompilationTime(): number;
    }

    export interface GarbageCollectorMXBean extends MemoryManagerMXBean {

        getCollectionCount(): number;

        getCollectionTime(): number;
    }

    export class LockInfo {
        constructor(arg0: String, arg1: number);

        getClassName(): String;

        getIdentityHashCode(): number;

        static from(arg0: CompositeData): LockInfo;
        toString(): string;
    }

    export class ManagementFactory {
        static CLASS_LOADING_MXBEAN_NAME: String
        static COMPILATION_MXBEAN_NAME: String
        static MEMORY_MXBEAN_NAME: String
        static OPERATING_SYSTEM_MXBEAN_NAME: String
        static RUNTIME_MXBEAN_NAME: String
        static THREAD_MXBEAN_NAME: String
        static GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE: String
        static MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE: String
        static MEMORY_POOL_MXBEAN_DOMAIN_TYPE: String

        static getClassLoadingMXBean(): ClassLoadingMXBean;

        static getMemoryMXBean(): MemoryMXBean;

        static getThreadMXBean(): ThreadMXBean;

        static getRuntimeMXBean(): RuntimeMXBean;

        static getCompilationMXBean(): CompilationMXBean;

        static getOperatingSystemMXBean(): OperatingSystemMXBean;

        static getMemoryPoolMXBeans(): List<MemoryPoolMXBean>;

        static getMemoryManagerMXBeans(): List<MemoryManagerMXBean>;

        static getGarbageCollectorMXBeans(): List<GarbageCollectorMXBean>;

        static getPlatformMBeanServer(): MBeanServer;

        static newPlatformMXBeanProxy<T extends Object>(arg0: MBeanServerConnection, arg1: String, arg2: Class<T>): T;

        static getPlatformMXBean<T extends PlatformManagedObject>(arg0: Class<T>): T;

        static getPlatformMXBeans<T extends PlatformManagedObject>(arg0: Class<T>): List<T>;

        static getPlatformMXBean<T extends PlatformManagedObject>(arg0: MBeanServerConnection, arg1: Class<T>): T;

        static getPlatformMXBeans<T extends PlatformManagedObject>(arg0: MBeanServerConnection, arg1: Class<T>): List<T>;

        static getPlatformManagementInterfaces(): Set<Class<PlatformManagedObject>>;
    }

    export class ManagementPermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export interface MemoryMXBean extends PlatformManagedObject {

        getObjectPendingFinalizationCount(): number;

        getHeapMemoryUsage(): MemoryUsage;

        getNonHeapMemoryUsage(): MemoryUsage;

        isVerbose(): boolean;

        setVerbose(arg0: boolean): void;

        gc(): void;
    }

    export interface MemoryManagerMXBean extends PlatformManagedObject {

        getName(): String;

        isValid(): boolean;

        getMemoryPoolNames(): String[];
    }

    export class MemoryNotificationInfo {
        static MEMORY_THRESHOLD_EXCEEDED: String
        static MEMORY_COLLECTION_THRESHOLD_EXCEEDED: String
        constructor(arg0: String, arg1: MemoryUsage, arg2: number);

        getPoolName(): String;

        getUsage(): MemoryUsage;

        getCount(): number;

        static from(arg0: CompositeData): MemoryNotificationInfo;
    }

    export interface MemoryPoolMXBean extends PlatformManagedObject {

        getName(): String;

        getType(): MemoryType;

        getUsage(): MemoryUsage;

        getPeakUsage(): MemoryUsage;

        resetPeakUsage(): void;

        isValid(): boolean;

        getMemoryManagerNames(): String[];

        getUsageThreshold(): number;

        setUsageThreshold(arg0: number): void;

        isUsageThresholdExceeded(): boolean;

        getUsageThresholdCount(): number;

        isUsageThresholdSupported(): boolean;

        getCollectionUsageThreshold(): number;

        setCollectionUsageThreshold(arg0: number): void;

        isCollectionUsageThresholdExceeded(): boolean;

        getCollectionUsageThresholdCount(): number;

        getCollectionUsage(): MemoryUsage;

        isCollectionUsageThresholdSupported(): boolean;
    }

    export class MemoryType extends Enum<MemoryType> {
        static HEAP: MemoryType
        static NON_HEAP: MemoryType

        static values(): MemoryType[];

        static valueOf(arg0: String): MemoryType;
        toString(): string;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class MemoryUsage {
        constructor(arg0: number, arg1: number, arg2: number, arg3: number);

        getInit(): number;

        getUsed(): number;

        getCommitted(): number;

        getMax(): number;
        toString(): string;

        static from(arg0: CompositeData): MemoryUsage;
    }

    export class MonitorInfo extends LockInfo {
        constructor(arg0: String, arg1: number, arg2: number, arg3: StackTraceElement);

        getLockedStackDepth(): number;

        getLockedStackFrame(): StackTraceElement;

        static from(arg0: CompositeData): MonitorInfo;
    }

    export interface OperatingSystemMXBean extends PlatformManagedObject {

        getName(): String;

        getArch(): String;

        getVersion(): String;

        getAvailableProcessors(): number;

        getSystemLoadAverage(): number;
    }

    export interface PlatformLoggingMXBean extends PlatformManagedObject {

        getLoggerNames(): List<String>;

        getLoggerLevel(arg0: String): String;

        setLoggerLevel(arg0: String, arg1: String): void;

        getParentLoggerName(arg0: String): String;
    }

    export interface PlatformManagedObject {

        getObjectName(): ObjectName;
    }

    export interface RuntimeMXBean extends PlatformManagedObject {

/* default */ getPid(): number;

        getName(): String;

        getVmName(): String;

        getVmVendor(): String;

        getVmVersion(): String;

        getSpecName(): String;

        getSpecVendor(): String;

        getSpecVersion(): String;

        getManagementSpecVersion(): String;

        getClassPath(): String;

        getLibraryPath(): String;

        isBootClassPathSupported(): boolean;

        getBootClassPath(): String;

        getInputArguments(): List<String>;

        getUptime(): number;

        getStartTime(): number;

        getSystemProperties(): Map<String, String>;
    }

    export class ThreadInfo {

        getThreadId(): number;

        getThreadName(): String;

        getThreadState(): Thread.State;

        getBlockedTime(): number;

        getBlockedCount(): number;

        getWaitedTime(): number;

        getWaitedCount(): number;

        getLockInfo(): LockInfo;

        getLockName(): String;

        getLockOwnerId(): number;

        getLockOwnerName(): String;

        getStackTrace(): StackTraceElement[];

        isSuspended(): boolean;

        isInNative(): boolean;

        isDaemon(): boolean;

        getPriority(): number;
        toString(): string;

        static from(arg0: CompositeData): ThreadInfo;

        getLockedMonitors(): MonitorInfo[];

        getLockedSynchronizers(): LockInfo[];
    }

    export interface ThreadMXBean extends PlatformManagedObject {

        getThreadCount(): number;

        getPeakThreadCount(): number;

        getTotalStartedThreadCount(): number;

        getDaemonThreadCount(): number;

        getAllThreadIds(): number[];

        getThreadInfo(arg0: number): ThreadInfo;

        getThreadInfo(arg0: number[]): ThreadInfo[];

        getThreadInfo(arg0: number, arg1: number): ThreadInfo;

        getThreadInfo(arg0: number[], arg1: number): ThreadInfo[];

        isThreadContentionMonitoringSupported(): boolean;

        isThreadContentionMonitoringEnabled(): boolean;

        setThreadContentionMonitoringEnabled(arg0: boolean): void;

        getCurrentThreadCpuTime(): number;

        getCurrentThreadUserTime(): number;

        getThreadCpuTime(arg0: number): number;

        getThreadUserTime(arg0: number): number;

        isThreadCpuTimeSupported(): boolean;

        isCurrentThreadCpuTimeSupported(): boolean;

        isThreadCpuTimeEnabled(): boolean;

        setThreadCpuTimeEnabled(arg0: boolean): void;

        findMonitorDeadlockedThreads(): number[];

        resetPeakThreadCount(): void;

        findDeadlockedThreads(): number[];

        isObjectMonitorUsageSupported(): boolean;

        isSynchronizerUsageSupported(): boolean;

        getThreadInfo(arg0: number[], arg1: boolean, arg2: boolean): ThreadInfo[];

/* default */ getThreadInfo(arg0: number[], arg1: boolean, arg2: boolean, arg3: number): ThreadInfo[];

        dumpAllThreads(arg0: boolean, arg1: boolean): ThreadInfo[];

/* default */ dumpAllThreads(arg0: boolean, arg1: boolean, arg2: number): ThreadInfo[];
    }

}
/// <reference path="java.lang.reflect.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.lang.constant.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.nio.d.ts" />
declare module '@java/java.lang.invoke' {
    import { Field, Method, Member, Constructor } from '@java/java.lang.reflect'
    import { Enum, RuntimeException, Throwable, ClassLoader, Class, String, Exception } from '@java/java.lang'
    import { MethodTypeDesc, MethodHandleDesc, Constable, DynamicConstantDesc, ClassDesc } from '@java/java.lang.constant'
    import { Optional, List } from '@java/java.util'
    import { Serializable } from '@java/java.io'
    import { ByteOrder } from '@java/java.nio'
    export abstract class CallSite {

        type(): MethodType;

        abstract getTarget(): MethodHandle;

        abstract setTarget(arg0: MethodHandle): void;

        abstract dynamicInvoker(): MethodHandle;
    }

    export class ConstantBootstraps {

        static nullConstant(arg0: Lookup, arg1: String, arg2: Class<any>): Object;

        static primitiveClass(arg0: Lookup, arg1: String, arg2: Class<any>): Class<any>;

        static enumConstant<E extends Enum<E>>(arg0: Lookup, arg1: String, arg2: Class<E>): E;

        static getStaticFinal(arg0: Lookup, arg1: String, arg2: Class<any>, arg3: Class<any>): Object;

        static getStaticFinal(arg0: Lookup, arg1: String, arg2: Class<any>): Object;

        static invoke(arg0: Lookup, arg1: String, arg2: Class<any>, arg3: MethodHandle, arg4: Object[]): Object;

        static fieldVarHandle(arg0: Lookup, arg1: String, arg2: Class<VarHandle>, arg3: Class<any>, arg4: Class<any>): VarHandle;

        static staticFieldVarHandle(arg0: Lookup, arg1: String, arg2: Class<VarHandle>, arg3: Class<any>, arg4: Class<any>): VarHandle;

        static arrayVarHandle(arg0: Lookup, arg1: String, arg2: Class<VarHandle>, arg3: Class<any>): VarHandle;

        static explicitCast(arg0: Lookup, arg1: String, arg2: Class<any>, arg3: Object): Object;
    }

    export class ConstantCallSite extends CallSite {
        constructor(arg0: MethodHandle);

        getTarget(): MethodHandle;

        setTarget(arg0: MethodHandle): void;

        dynamicInvoker(): MethodHandle;
    }

    export class LambdaConversionException extends Exception {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable, arg2: boolean, arg3: boolean);
    }

    export class LambdaMetafactory {
        static FLAG_SERIALIZABLE: number
        static FLAG_MARKERS: number
        static FLAG_BRIDGES: number

        static metafactory(arg0: Lookup, arg1: String, arg2: MethodType, arg3: MethodType, arg4: MethodHandle, arg5: MethodType): CallSite;

        static altMetafactory(arg0: Lookup, arg1: String, arg2: MethodType, arg3: Object[]): CallSite;
    }

    export abstract class MethodHandle implements Constable {

        type(): MethodType;

        invokeExact(arg0: Object[]): Object;

        invoke(arg0: Object[]): Object;

        invokeWithArguments(arg0: Object[]): Object;

        invokeWithArguments(arg0: List<any>): Object;

        asType(arg0: MethodType): MethodHandle;

        asSpreader(arg0: Class<any>, arg1: number): MethodHandle;

        asSpreader(arg0: number, arg1: Class<any>, arg2: number): MethodHandle;

        withVarargs(arg0: boolean): MethodHandle;

        asCollector(arg0: Class<any>, arg1: number): MethodHandle;

        asCollector(arg0: number, arg1: Class<any>, arg2: number): MethodHandle;

        asVarargsCollector(arg0: Class<any>): MethodHandle;

        isVarargsCollector(): boolean;

        asFixedArity(): MethodHandle;

        bindTo(arg0: Object): MethodHandle;

        describeConstable(): Optional<MethodHandleDesc>;
        toString(): string;
    }

    export namespace MethodHandleInfo {
        function
/* default */ referenceKindToString(arg0: number): String;
        function
/* default */ toString(arg0: number, arg1: Class<any>, arg2: String, arg3: MethodType): String;
        const REF_getField: number
        const REF_getStatic: number
        const REF_putField: number
        const REF_putStatic: number
        const REF_invokeVirtual: number
        const REF_invokeStatic: number
        const REF_invokeSpecial: number
        const REF_newInvokeSpecial: number
        const REF_invokeInterface: number
    }

    export interface MethodHandleInfo {
        REF_getField: number
        REF_getStatic: number
        REF_putField: number
        REF_putStatic: number
        REF_invokeVirtual: number
        REF_invokeStatic: number
        REF_invokeSpecial: number
        REF_newInvokeSpecial: number
        REF_invokeInterface: number

        getReferenceKind(): number;

        getDeclaringClass(): Class<any>;

        getName(): String;

        getMethodType(): MethodType;

        reflectAs<T extends Member>(arg0: Class<T>, arg1: Lookup): T;

        getModifiers(): number;

/* default */ isVarArgs(): boolean;
    }

    export class MethodHandleProxies {

        static asInterfaceInstance<T extends Object>(arg0: Class<T>, arg1: MethodHandle): T;

        static isWrapperInstance(arg0: Object): boolean;

        static wrapperInstanceTarget(arg0: Object): MethodHandle;

        static wrapperInstanceType(arg0: Object): Class<any>;
    }

    export class MethodHandles {

        static lookup(): Lookup;

        static publicLookup(): Lookup;

        static privateLookupIn(arg0: Class<any>, arg1: Lookup): Lookup;

        static classData<T extends Object>(arg0: Lookup, arg1: String, arg2: Class<T>): T;

        static classDataAt<T extends Object>(arg0: Lookup, arg1: String, arg2: Class<T>, arg3: number): T;

        static reflectAs<T extends Member>(arg0: Class<T>, arg1: MethodHandle): T;

        static arrayConstructor(arg0: Class<any>): MethodHandle;

        static arrayLength(arg0: Class<any>): MethodHandle;

        static arrayElementGetter(arg0: Class<any>): MethodHandle;

        static arrayElementSetter(arg0: Class<any>): MethodHandle;

        static arrayElementVarHandle(arg0: Class<any>): VarHandle;

        static byteArrayViewVarHandle(arg0: Class<any>, arg1: ByteOrder): VarHandle;

        static byteBufferViewVarHandle(arg0: Class<any>, arg1: ByteOrder): VarHandle;

        static spreadInvoker(arg0: MethodType, arg1: number): MethodHandle;

        static exactInvoker(arg0: MethodType): MethodHandle;

        static invoker(arg0: MethodType): MethodHandle;

        static varHandleExactInvoker(arg0: VarHandle.AccessMode, arg1: MethodType): MethodHandle;

        static varHandleInvoker(arg0: VarHandle.AccessMode, arg1: MethodType): MethodHandle;

        static explicitCastArguments(arg0: MethodHandle, arg1: MethodType): MethodHandle;

        static permuteArguments(arg0: MethodHandle, arg1: MethodType, arg2: number[]): MethodHandle;

        static constant(arg0: Class<any>, arg1: Object): MethodHandle;

        static identity(arg0: Class<any>): MethodHandle;

        static zero(arg0: Class<any>): MethodHandle;

        static empty(arg0: MethodType): MethodHandle;

        static insertArguments(arg0: MethodHandle, arg1: number, arg2: Object[]): MethodHandle;

        static dropArguments(arg0: MethodHandle, arg1: number, arg2: List<Class<any>>): MethodHandle;

        static dropArguments(arg0: MethodHandle, arg1: number, arg2: Class<any>[]): MethodHandle;

        static dropArgumentsToMatch(arg0: MethodHandle, arg1: number, arg2: List<Class<any>>, arg3: number): MethodHandle;

        static dropReturn(arg0: MethodHandle): MethodHandle;

        static filterArguments(arg0: MethodHandle, arg1: number, arg2: MethodHandle[]): MethodHandle;

        static collectArguments(arg0: MethodHandle, arg1: number, arg2: MethodHandle): MethodHandle;

        static filterReturnValue(arg0: MethodHandle, arg1: MethodHandle): MethodHandle;

        static foldArguments(arg0: MethodHandle, arg1: MethodHandle): MethodHandle;

        static foldArguments(arg0: MethodHandle, arg1: number, arg2: MethodHandle): MethodHandle;

        static guardWithTest(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle): MethodHandle;

        static catchException(arg0: MethodHandle, arg1: Class<Throwable>, arg2: MethodHandle): MethodHandle;

        static throwException(arg0: Class<any>, arg1: Class<Throwable>): MethodHandle;

        static loop(arg0: Array<Array<MethodHandle>>): MethodHandle;

        static whileLoop(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle): MethodHandle;

        static doWhileLoop(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle): MethodHandle;

        static countedLoop(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle): MethodHandle;

        static countedLoop(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle, arg3: MethodHandle): MethodHandle;

        static iteratedLoop(arg0: MethodHandle, arg1: MethodHandle, arg2: MethodHandle): MethodHandle;

        static tryFinally(arg0: MethodHandle, arg1: MethodHandle): MethodHandle;

        static tableSwitch(arg0: MethodHandle, arg1: MethodHandle[]): MethodHandle;
    }

    export class Lookup {
        static PUBLIC: number
        static PRIVATE: number
        static PROTECTED: number
        static PACKAGE: number
        static MODULE: number
        static UNCONDITIONAL: number
        static ORIGINAL: number

        lookupClass(): Class<any>;

        previousLookupClass(): Class<any>;

        lookupModes(): number;

        in(arg0: Class<any>): Lookup;

        dropLookupMode(arg0: number): Lookup;

        defineClass(arg0: number[]): Class<any>;

        defineHiddenClass(arg0: number[], arg1: boolean, arg2: MethodHandles.Lookup.ClassOption[]): Lookup;

        defineHiddenClassWithClassData(arg0: number[], arg1: Object, arg2: boolean, arg3: MethodHandles.Lookup.ClassOption[]): Lookup;
        toString(): string;

        findStatic(arg0: Class<any>, arg1: String, arg2: MethodType): MethodHandle;

        findVirtual(arg0: Class<any>, arg1: String, arg2: MethodType): MethodHandle;

        findConstructor(arg0: Class<any>, arg1: MethodType): MethodHandle;

        findClass(arg0: String): Class<any>;

        ensureInitialized(arg0: Class<any>): Class<any>;

        accessClass(arg0: Class<any>): Class<any>;

        findSpecial(arg0: Class<any>, arg1: String, arg2: MethodType, arg3: Class<any>): MethodHandle;

        findGetter(arg0: Class<any>, arg1: String, arg2: Class<any>): MethodHandle;

        findSetter(arg0: Class<any>, arg1: String, arg2: Class<any>): MethodHandle;

        findVarHandle(arg0: Class<any>, arg1: String, arg2: Class<any>): VarHandle;

        findStaticGetter(arg0: Class<any>, arg1: String, arg2: Class<any>): MethodHandle;

        findStaticSetter(arg0: Class<any>, arg1: String, arg2: Class<any>): MethodHandle;

        findStaticVarHandle(arg0: Class<any>, arg1: String, arg2: Class<any>): VarHandle;

        bind(arg0: Object, arg1: String, arg2: MethodType): MethodHandle;

        unreflect(arg0: Method): MethodHandle;

        unreflectSpecial(arg0: Method, arg1: Class<any>): MethodHandle;

        unreflectConstructor(arg0: Constructor<any>): MethodHandle;

        unreflectGetter(arg0: Field): MethodHandle;

        unreflectSetter(arg0: Field): MethodHandle;

        unreflectVarHandle(arg0: Field): VarHandle;

        revealDirect(arg0: MethodHandle): MethodHandleInfo;

        hasPrivateAccess(): boolean;

        hasFullPrivilegeAccess(): boolean;
    }
    export namespace Lookup {
        export class ClassOption extends Enum<MethodHandles.Lookup.ClassOption> {
            static NESTMATE: MethodHandles.Lookup.ClassOption
            static STRONG: MethodHandles.Lookup.ClassOption

            static values(): MethodHandles.Lookup.ClassOption[];

            static valueOf(arg0: String): MethodHandles.Lookup.ClassOption;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class MethodType extends Object implements Constable, TypeDescriptor.OfMethod<Class<any>, MethodType>, Serializable {

        static methodType(arg0: Class<any>, arg1: Class<any>[]): MethodType;

        static methodType(arg0: Class<any>, arg1: List<Class<any>>): MethodType;

        static methodType(arg0: Class<any>, arg1: Class<any>, arg2: Class<any>[]): MethodType;

        static methodType(arg0: Class<any>): MethodType;

        static methodType(arg0: Class<any>, arg1: Class<any>): MethodType;

        static methodType(arg0: Class<any>, arg1: MethodType): MethodType;

        static genericMethodType(arg0: number, arg1: boolean): MethodType;

        static genericMethodType(arg0: number): MethodType;

        changeParameterType(arg0: number, arg1: Class<any>): MethodType;

        insertParameterTypes(arg0: number, arg1: Class<any>[]): MethodType;

        appendParameterTypes(arg0: Class<any>[]): MethodType;

        insertParameterTypes(arg0: number, arg1: List<Class<any>>): MethodType;

        appendParameterTypes(arg0: List<Class<any>>): MethodType;

        dropParameterTypes(arg0: number, arg1: number): MethodType;

        changeReturnType(arg0: Class<any>): MethodType;

        hasPrimitives(): boolean;

        hasWrappers(): boolean;

        erase(): MethodType;

        generic(): MethodType;

        wrap(): MethodType;

        unwrap(): MethodType;

        parameterType(arg0: number): Class<any>;

        parameterCount(): number;

        returnType(): Class<any>;

        parameterList(): List<Class<any>>;

        lastParameterType(): Class<any>;

        parameterArray(): Class<any>[];

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        static fromMethodDescriptorString(arg0: String, arg1: ClassLoader): MethodType;

        toMethodDescriptorString(): String;

        descriptorString(): String;

        describeConstable(): Optional<MethodTypeDesc>;
    }

    export class MutableCallSite extends CallSite {
        constructor(arg0: MethodType);
        constructor(arg0: MethodHandle);

        getTarget(): MethodHandle;

        setTarget(arg0: MethodHandle): void;

        dynamicInvoker(): MethodHandle;

        static syncAll(arg0: MutableCallSite[]): void;
    }

    export class SerializedLambda implements Serializable {
        constructor(arg0: Class<any>, arg1: String, arg2: String, arg3: String, arg4: number, arg5: String, arg6: String, arg7: String, arg8: String, arg9: Object[]);

        getCapturingClass(): String;

        getFunctionalInterfaceClass(): String;

        getFunctionalInterfaceMethodName(): String;

        getFunctionalInterfaceMethodSignature(): String;

        getImplClass(): String;

        getImplMethodName(): String;

        getImplMethodSignature(): String;

        getImplMethodKind(): number;

        getInstantiatedMethodType(): String;

        getCapturedArgCount(): number;

        getCapturedArg(arg0: number): Object;
        toString(): string;
    }

    export class StringConcatException extends Exception {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export class StringConcatFactory {

        static makeConcat(arg0: Lookup, arg1: String, arg2: MethodType): CallSite;

        static makeConcatWithConstants(arg0: Lookup, arg1: String, arg2: MethodType, arg3: String, arg4: Object[]): CallSite;
    }

    export class SwitchPoint {
        constructor();

        hasBeenInvalidated(): boolean;

        guardWithTest(arg0: MethodHandle, arg1: MethodHandle): MethodHandle;

        static invalidateAll(arg0: SwitchPoint[]): void;
    }

    export interface TypeDescriptor {

        descriptorString(): String;
    }
    export namespace TypeDescriptor {
        export interface OfField<F extends TypeDescriptor.OfField<F>> extends TypeDescriptor, Object {

            isArray(): boolean;

            isPrimitive(): boolean;

            componentType(): F;

            arrayType(): F;
        }

        export interface OfMethod<F extends TypeDescriptor.OfField<F>, M extends TypeDescriptor.OfMethod<F, M>> extends TypeDescriptor, Object {

            parameterCount(): number;

            parameterType(arg0: number): F;

            returnType(): F;

            parameterArray(): F[];

            parameterList(): List<F>;

            changeReturnType(arg0: F): M;

            changeParameterType(arg0: number, arg1: F): M;

            dropParameterTypes(arg0: number, arg1: number): M;

            insertParameterTypes(arg0: number, arg1: F[]): M;
        }

    }

    export abstract class VarHandle implements Constable {

        hasInvokeExactBehavior(): boolean;

        get(arg0: Object[]): Object;

        set(arg0: Object[]): void;

        getVolatile(arg0: Object[]): Object;

        setVolatile(arg0: Object[]): void;

        getOpaque(arg0: Object[]): Object;

        setOpaque(arg0: Object[]): void;

        getAcquire(arg0: Object[]): Object;

        setRelease(arg0: Object[]): void;

        compareAndSet(arg0: Object[]): boolean;

        compareAndExchange(arg0: Object[]): Object;

        compareAndExchangeAcquire(arg0: Object[]): Object;

        compareAndExchangeRelease(arg0: Object[]): Object;

        weakCompareAndSetPlain(arg0: Object[]): boolean;

        weakCompareAndSet(arg0: Object[]): boolean;

        weakCompareAndSetAcquire(arg0: Object[]): boolean;

        weakCompareAndSetRelease(arg0: Object[]): boolean;

        getAndSet(arg0: Object[]): Object;

        getAndSetAcquire(arg0: Object[]): Object;

        getAndSetRelease(arg0: Object[]): Object;

        getAndAdd(arg0: Object[]): Object;

        getAndAddAcquire(arg0: Object[]): Object;

        getAndAddRelease(arg0: Object[]): Object;

        getAndBitwiseOr(arg0: Object[]): Object;

        getAndBitwiseOrAcquire(arg0: Object[]): Object;

        getAndBitwiseOrRelease(arg0: Object[]): Object;

        getAndBitwiseAnd(arg0: Object[]): Object;

        getAndBitwiseAndAcquire(arg0: Object[]): Object;

        getAndBitwiseAndRelease(arg0: Object[]): Object;

        getAndBitwiseXor(arg0: Object[]): Object;

        getAndBitwiseXorAcquire(arg0: Object[]): Object;

        getAndBitwiseXorRelease(arg0: Object[]): Object;

        abstract withInvokeExactBehavior(): VarHandle;

        abstract withInvokeBehavior(): VarHandle;
        toString(): string;

        varType(): Class<any>;

        coordinateTypes(): List<Class<any>>;

        accessModeType(arg0: VarHandle.AccessMode): MethodType;

        isAccessModeSupported(arg0: VarHandle.AccessMode): boolean;

        toMethodHandle(arg0: VarHandle.AccessMode): MethodHandle;

        describeConstable(): Optional<VarHandle.VarHandleDesc>;

        static fullFence(): void;

        static acquireFence(): void;

        static releaseFence(): void;

        static loadLoadFence(): void;

        static storeStoreFence(): void;
    }
    export namespace VarHandle {
        export class AccessMode extends Enum<VarHandle.AccessMode> {
            static GET: VarHandle.AccessMode
            static SET: VarHandle.AccessMode
            static GET_VOLATILE: VarHandle.AccessMode
            static SET_VOLATILE: VarHandle.AccessMode
            static GET_ACQUIRE: VarHandle.AccessMode
            static SET_RELEASE: VarHandle.AccessMode
            static GET_OPAQUE: VarHandle.AccessMode
            static SET_OPAQUE: VarHandle.AccessMode
            static COMPARE_AND_SET: VarHandle.AccessMode
            static COMPARE_AND_EXCHANGE: VarHandle.AccessMode
            static COMPARE_AND_EXCHANGE_ACQUIRE: VarHandle.AccessMode
            static COMPARE_AND_EXCHANGE_RELEASE: VarHandle.AccessMode
            static WEAK_COMPARE_AND_SET_PLAIN: VarHandle.AccessMode
            static WEAK_COMPARE_AND_SET: VarHandle.AccessMode
            static WEAK_COMPARE_AND_SET_ACQUIRE: VarHandle.AccessMode
            static WEAK_COMPARE_AND_SET_RELEASE: VarHandle.AccessMode
            static GET_AND_SET: VarHandle.AccessMode
            static GET_AND_SET_ACQUIRE: VarHandle.AccessMode
            static GET_AND_SET_RELEASE: VarHandle.AccessMode
            static GET_AND_ADD: VarHandle.AccessMode
            static GET_AND_ADD_ACQUIRE: VarHandle.AccessMode
            static GET_AND_ADD_RELEASE: VarHandle.AccessMode
            static GET_AND_BITWISE_OR: VarHandle.AccessMode
            static GET_AND_BITWISE_OR_RELEASE: VarHandle.AccessMode
            static GET_AND_BITWISE_OR_ACQUIRE: VarHandle.AccessMode
            static GET_AND_BITWISE_AND: VarHandle.AccessMode
            static GET_AND_BITWISE_AND_RELEASE: VarHandle.AccessMode
            static GET_AND_BITWISE_AND_ACQUIRE: VarHandle.AccessMode
            static GET_AND_BITWISE_XOR: VarHandle.AccessMode
            static GET_AND_BITWISE_XOR_RELEASE: VarHandle.AccessMode
            static GET_AND_BITWISE_XOR_ACQUIRE: VarHandle.AccessMode

            static values(): VarHandle.AccessMode[];

            static valueOf(arg0: String): VarHandle.AccessMode;

            methodName(): String;

            static valueFromMethodName(arg0: String): VarHandle.AccessMode;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export class VarHandleDesc extends DynamicConstantDesc<VarHandle> {

            static ofField(arg0: ClassDesc, arg1: String, arg2: ClassDesc): VarHandle.VarHandleDesc;

            static ofStaticField(arg0: ClassDesc, arg1: String, arg2: ClassDesc): VarHandle.VarHandleDesc;

            static ofArray(arg0: ClassDesc): VarHandle.VarHandleDesc;

            varType(): ClassDesc;

            resolveConstantDesc(arg0: Lookup): VarHandle;
            toString(): string;
        }

    }

    export class VolatileCallSite extends CallSite {
        constructor(arg0: MethodType);
        constructor(arg0: MethodHandle);

        getTarget(): MethodHandle;

        setTarget(arg0: MethodHandle): void;

        dynamicInvoker(): MethodHandle;
    }

    export class WrongMethodTypeException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.util.jar.d.ts" />
declare module '@java/java.lang.instrument' {
    import { ProtectionDomain } from '@java/java.security'
    import { ClassLoader, Class, String, RuntimeException, Module, Exception } from '@java/java.lang'
    import { Map, List, Set } from '@java/java.util'
    import { JarFile } from '@java/java.util.jar'
    export class ClassDefinition {
        constructor(arg0: Class<any>, arg1: number[]);

        getDefinitionClass(): Class<any>;

        getDefinitionClassFile(): number[];
    }

    export interface ClassFileTransformer {

/* default */ transform(arg0: ClassLoader, arg1: String, arg2: Class<any>, arg3: ProtectionDomain, arg4: number[]): number[];

/* default */ transform(arg0: Module, arg1: ClassLoader, arg2: String, arg3: Class<any>, arg4: ProtectionDomain, arg5: number[]): number[];
    }

    export class IllegalClassFormatException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface Instrumentation {

        addTransformer(arg0: ClassFileTransformer, arg1: boolean): void;

        addTransformer(arg0: ClassFileTransformer): void;

        removeTransformer(arg0: ClassFileTransformer): boolean;

        isRetransformClassesSupported(): boolean;

        retransformClasses(arg0: Class<any>[]): void;

        isRedefineClassesSupported(): boolean;

        redefineClasses(arg0: ClassDefinition[]): void;

        isModifiableClass(arg0: Class<any>): boolean;

        getAllLoadedClasses(): Class[];

        getInitiatedClasses(arg0: ClassLoader): Class[];

        getObjectSize(arg0: Object): number;

        appendToBootstrapClassLoaderSearch(arg0: JarFile): void;

        appendToSystemClassLoaderSearch(arg0: JarFile): void;

        isNativeMethodPrefixSupported(): boolean;

        setNativeMethodPrefix(arg0: ClassFileTransformer, arg1: String): void;

        redefineModule(arg0: Module, arg1: Set<Module>, arg2: Map<String, Set<Module>>, arg3: Map<String, Set<Module>>, arg4: Set<Class<any>>, arg5: Map<Class<any>, List<Class<any>>>): void;

        isModifiableModule(arg0: Module): boolean;
    }

    export class UnmodifiableClassException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export class UnmodifiableModuleException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

}
/// <reference path="java.lang.reflect.d.ts" />
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.constant.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.util.concurrent.d.ts" />
/// <reference path="java.lang.annotation.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="java.lang.module.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
/// <reference path="java.time.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.io.d.ts" />
/// <reference path="java.lang.invoke.d.ts" />
/// <reference path="java.util.function.d.ts" />
declare module '@java/java.lang' {
    import { Field, Type, AnnotatedType, TypeVariable, Constructor, AnnotatedElement, Method, GenericDeclaration, RecordComponent } from '@java/java.lang.reflect'
    import { Permission, ProtectionDomain, BasicPermission } from '@java/java.security'
    import { Constable, DynamicConstantDesc, ConstantDesc, ClassDesc } from '@java/java.lang.constant'
    import { Locale, Enumeration, Set, Optional, Iterator, List, ResourceBundle, Properties, Map, Spliterator, Comparator } from '@java/java.util'
    import { IntStream, Stream } from '@java/java.util.stream'
    import { TimeUnit, CompletableFuture } from '@java/java.util.concurrent'
    import { Annotation } from '@java/java.lang.annotation'
    import { CharBuffer } from '@java/java.nio'
    import { Channel } from '@java/java.nio.channels'
    import { Configuration, ModuleDescriptor } from '@java/java.lang.module'
    import { Charset } from '@java/java.nio.charset'
    import { Duration, Instant } from '@java/java.time'
    import { InetAddress, URL } from '@java/java.net'
    import { PrintStream, Serializable, InputStream, OutputStream, BufferedReader, Console, File, FileDescriptor, BufferedWriter, PrintWriter } from '@java/java.io'
    import { TypeDescriptor, MethodType, Lookup } from '@java/java.lang.invoke'
    import { Consumer, Function, Supplier } from '@java/java.util.function'
    export class AbstractMethodError extends IncompatibleClassChangeError {
        constructor();
        constructor(arg0: String);
    }

    export interface Appendable {

        append(arg0: CharSequence): Appendable;

        append(arg0: CharSequence, arg1: number, arg2: number): Appendable;

        append(arg0: String): Appendable;
    }

    export class ArithmeticException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: number);
    }

    export class ArrayStoreException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class AssertionError extends Error {
        constructor();
        constructor(arg0: Object);
        constructor(arg0: boolean);
        constructor(arg0: String);
        constructor(arg0: number);
        constructor(arg0: number);
        constructor(arg0: number);
        constructor(arg0: number);
        constructor(arg0: String, arg1: Throwable);
    }

    export interface AutoCloseable {

        close(): void;
    }

    export class Boolean extends Object implements Serializable, Comparable<Boolean>, Constable {
        static TRUE: Boolean
        static FALSE: Boolean
        static TYPE: Class<Boolean>
        constructor(arg0: boolean);
        constructor(arg0: String);

        static parseBoolean(arg0: String): boolean;

        booleanValue(): boolean;

        static valueOf(arg0: boolean): Boolean;

        static valueOf(arg0: String): Boolean;

        static toString(arg0: boolean): String;
        toString(): string;

        hashCode(): number;

        static hashCode(arg0: boolean): number;

        equals(arg0: Object): boolean;

        static getBoolean(arg0: String): boolean;

        compareTo(arg0: Boolean): number;

        static compare(arg0: boolean, arg1: boolean): number;

        static logicalAnd(arg0: boolean, arg1: boolean): boolean;

        static logicalOr(arg0: boolean, arg1: boolean): boolean;

        static logicalXor(arg0: boolean, arg1: boolean): boolean;

        describeConstable(): Optional<DynamicConstantDesc<Boolean>>;
    }

    export class BootstrapMethodError extends LinkageError {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class Byte extends Number implements Comparable<Number>, Constable {
        static MIN_VALUE: number
        static MAX_VALUE: number
        static TYPE: Class<Number>
        static SIZE: number
        static BYTES: number
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number): String;

        describeConstable(): Optional<DynamicConstantDesc<Number>>;

        static valueOf(arg0: number): Number;

        static parseByte(arg0: String, arg1: number): number;

        static parseByte(arg0: String): number;

        static valueOf(arg0: String, arg1: number): Number;

        static valueOf(arg0: String): Number;

        static decode(arg0: String): Number;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;
        toString(): string;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static compareUnsigned(arg0: number, arg1: number): number;

        static toUnsignedInt(arg0: number): number;

        static toUnsignedLong(arg0: number): number;
    }

    export namespace CharSequence {
        function
/* default */ compare(arg0: CharSequence, arg1: CharSequence): number;
    }

    export interface CharSequence {

        length(): number;

        charAt(arg0: number): String;

/* default */ isEmpty(): boolean;

        subSequence(arg0: number, arg1: number): CharSequence;
        toString(): string;

/* default */ chars(): IntStream;

/* default */ codePoints(): IntStream;
    }

    export class Character extends Object implements Serializable, Comparable<String>, Constable {
        static MIN_RADIX: number
        static MAX_RADIX: number
        static MIN_VALUE: String
        static MAX_VALUE: String
        static TYPE: Class<String>
        static UNASSIGNED: number
        static UPPERCASE_LETTER: number
        static LOWERCASE_LETTER: number
        static TITLECASE_LETTER: number
        static MODIFIER_LETTER: number
        static OTHER_LETTER: number
        static NON_SPACING_MARK: number
        static ENCLOSING_MARK: number
        static COMBINING_SPACING_MARK: number
        static DECIMAL_DIGIT_NUMBER: number
        static LETTER_NUMBER: number
        static OTHER_NUMBER: number
        static SPACE_SEPARATOR: number
        static LINE_SEPARATOR: number
        static PARAGRAPH_SEPARATOR: number
        static CONTROL: number
        static FORMAT: number
        static PRIVATE_USE: number
        static SURROGATE: number
        static DASH_PUNCTUATION: number
        static START_PUNCTUATION: number
        static END_PUNCTUATION: number
        static CONNECTOR_PUNCTUATION: number
        static OTHER_PUNCTUATION: number
        static MATH_SYMBOL: number
        static CURRENCY_SYMBOL: number
        static MODIFIER_SYMBOL: number
        static OTHER_SYMBOL: number
        static INITIAL_QUOTE_PUNCTUATION: number
        static FINAL_QUOTE_PUNCTUATION: number
        static DIRECTIONALITY_UNDEFINED: number
        static DIRECTIONALITY_LEFT_TO_RIGHT: number
        static DIRECTIONALITY_RIGHT_TO_LEFT: number
        static DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC: number
        static DIRECTIONALITY_EUROPEAN_NUMBER: number
        static DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR: number
        static DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR: number
        static DIRECTIONALITY_ARABIC_NUMBER: number
        static DIRECTIONALITY_COMMON_NUMBER_SEPARATOR: number
        static DIRECTIONALITY_NONSPACING_MARK: number
        static DIRECTIONALITY_BOUNDARY_NEUTRAL: number
        static DIRECTIONALITY_PARAGRAPH_SEPARATOR: number
        static DIRECTIONALITY_SEGMENT_SEPARATOR: number
        static DIRECTIONALITY_WHITESPACE: number
        static DIRECTIONALITY_OTHER_NEUTRALS: number
        static DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING: number
        static DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE: number
        static DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING: number
        static DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE: number
        static DIRECTIONALITY_POP_DIRECTIONAL_FORMAT: number
        static DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE: number
        static DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE: number
        static DIRECTIONALITY_FIRST_STRONG_ISOLATE: number
        static DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE: number
        static MIN_HIGH_SURROGATE: String
        static MAX_HIGH_SURROGATE: String
        static MIN_LOW_SURROGATE: String
        static MAX_LOW_SURROGATE: String
        static MIN_SURROGATE: String
        static MAX_SURROGATE: String
        static MIN_SUPPLEMENTARY_CODE_POINT: number
        static MIN_CODE_POINT: number
        static MAX_CODE_POINT: number
        static SIZE: number
        static BYTES: number
        constructor(arg0: String);

        describeConstable(): Optional<DynamicConstantDesc<String>>;

        static valueOf(arg0: String): String;

        charValue(): String;

        hashCode(): number;

        static hashCode(arg0: String): number;

        equals(arg0: Object): boolean;
        toString(): string;

        static toString(arg0: String): String;

        static toString(arg0: number): String;

        static isValidCodePoint(arg0: number): boolean;

        static isBmpCodePoint(arg0: number): boolean;

        static isSupplementaryCodePoint(arg0: number): boolean;

        static isHighSurrogate(arg0: String): boolean;

        static isLowSurrogate(arg0: String): boolean;

        static isSurrogate(arg0: String): boolean;

        static isSurrogatePair(arg0: String, arg1: String): boolean;

        static charCount(arg0: number): number;

        static toCodePoint(arg0: String, arg1: String): number;

        static codePointAt(arg0: CharSequence, arg1: number): number;

        static codePointAt(arg0: String[], arg1: number): number;

        static codePointAt(arg0: String[], arg1: number, arg2: number): number;

        static codePointBefore(arg0: CharSequence, arg1: number): number;

        static codePointBefore(arg0: String[], arg1: number): number;

        static codePointBefore(arg0: String[], arg1: number, arg2: number): number;

        static highSurrogate(arg0: number): String;

        static lowSurrogate(arg0: number): String;

        static toChars(arg0: number, arg1: String[], arg2: number): number;

        static toChars(arg0: number): String[];

        static codePointCount(arg0: CharSequence, arg1: number, arg2: number): number;

        static codePointCount(arg0: String[], arg1: number, arg2: number): number;

        static offsetByCodePoints(arg0: CharSequence, arg1: number, arg2: number): number;

        static offsetByCodePoints(arg0: String[], arg1: number, arg2: number, arg3: number, arg4: number): number;

        static isLowerCase(arg0: String): boolean;

        static isLowerCase(arg0: number): boolean;

        static isUpperCase(arg0: String): boolean;

        static isUpperCase(arg0: number): boolean;

        static isTitleCase(arg0: String): boolean;

        static isTitleCase(arg0: number): boolean;

        static isDigit(arg0: String): boolean;

        static isDigit(arg0: number): boolean;

        static isDefined(arg0: String): boolean;

        static isDefined(arg0: number): boolean;

        static isLetter(arg0: String): boolean;

        static isLetter(arg0: number): boolean;

        static isLetterOrDigit(arg0: String): boolean;

        static isLetterOrDigit(arg0: number): boolean;

        static isJavaLetter(arg0: String): boolean;

        static isJavaLetterOrDigit(arg0: String): boolean;

        static isAlphabetic(arg0: number): boolean;

        static isIdeographic(arg0: number): boolean;

        static isJavaIdentifierStart(arg0: String): boolean;

        static isJavaIdentifierStart(arg0: number): boolean;

        static isJavaIdentifierPart(arg0: String): boolean;

        static isJavaIdentifierPart(arg0: number): boolean;

        static isUnicodeIdentifierStart(arg0: String): boolean;

        static isUnicodeIdentifierStart(arg0: number): boolean;

        static isUnicodeIdentifierPart(arg0: String): boolean;

        static isUnicodeIdentifierPart(arg0: number): boolean;

        static isIdentifierIgnorable(arg0: String): boolean;

        static isIdentifierIgnorable(arg0: number): boolean;

        static toLowerCase(arg0: String): String;

        static toLowerCase(arg0: number): number;

        static toUpperCase(arg0: String): String;

        static toUpperCase(arg0: number): number;

        static toTitleCase(arg0: String): String;

        static toTitleCase(arg0: number): number;

        static digit(arg0: String, arg1: number): number;

        static digit(arg0: number, arg1: number): number;

        static getNumericValue(arg0: String): number;

        static getNumericValue(arg0: number): number;

        static isSpace(arg0: String): boolean;

        static isSpaceChar(arg0: String): boolean;

        static isSpaceChar(arg0: number): boolean;

        static isWhitespace(arg0: String): boolean;

        static isWhitespace(arg0: number): boolean;

        static isISOControl(arg0: String): boolean;

        static isISOControl(arg0: number): boolean;

        static getType(arg0: String): number;

        static getType(arg0: number): number;

        static forDigit(arg0: number, arg1: number): String;

        static getDirectionality(arg0: String): number;

        static getDirectionality(arg0: number): number;

        static isMirrored(arg0: String): boolean;

        static isMirrored(arg0: number): boolean;

        compareTo(arg0: String): number;

        static compare(arg0: String, arg1: String): number;

        static reverseBytes(arg0: String): String;

        static getName(arg0: number): String;

        static codePointOf(arg0: String): number;
    }
    export namespace Character {
        export class Subset {

            equals(arg0: Object): boolean;

            hashCode(): number;
            toString(): string;
        }

        export class UnicodeBlock extends Character.Subset {
            static BASIC_LATIN: Character.UnicodeBlock
            static LATIN_1_SUPPLEMENT: Character.UnicodeBlock
            static LATIN_EXTENDED_A: Character.UnicodeBlock
            static LATIN_EXTENDED_B: Character.UnicodeBlock
            static IPA_EXTENSIONS: Character.UnicodeBlock
            static SPACING_MODIFIER_LETTERS: Character.UnicodeBlock
            static COMBINING_DIACRITICAL_MARKS: Character.UnicodeBlock
            static GREEK: Character.UnicodeBlock
            static CYRILLIC: Character.UnicodeBlock
            static ARMENIAN: Character.UnicodeBlock
            static HEBREW: Character.UnicodeBlock
            static ARABIC: Character.UnicodeBlock
            static DEVANAGARI: Character.UnicodeBlock
            static BENGALI: Character.UnicodeBlock
            static GURMUKHI: Character.UnicodeBlock
            static GUJARATI: Character.UnicodeBlock
            static ORIYA: Character.UnicodeBlock
            static TAMIL: Character.UnicodeBlock
            static TELUGU: Character.UnicodeBlock
            static KANNADA: Character.UnicodeBlock
            static MALAYALAM: Character.UnicodeBlock
            static THAI: Character.UnicodeBlock
            static LAO: Character.UnicodeBlock
            static TIBETAN: Character.UnicodeBlock
            static GEORGIAN: Character.UnicodeBlock
            static HANGUL_JAMO: Character.UnicodeBlock
            static LATIN_EXTENDED_ADDITIONAL: Character.UnicodeBlock
            static GREEK_EXTENDED: Character.UnicodeBlock
            static GENERAL_PUNCTUATION: Character.UnicodeBlock
            static SUPERSCRIPTS_AND_SUBSCRIPTS: Character.UnicodeBlock
            static CURRENCY_SYMBOLS: Character.UnicodeBlock
            static COMBINING_MARKS_FOR_SYMBOLS: Character.UnicodeBlock
            static LETTERLIKE_SYMBOLS: Character.UnicodeBlock
            static NUMBER_FORMS: Character.UnicodeBlock
            static ARROWS: Character.UnicodeBlock
            static MATHEMATICAL_OPERATORS: Character.UnicodeBlock
            static MISCELLANEOUS_TECHNICAL: Character.UnicodeBlock
            static CONTROL_PICTURES: Character.UnicodeBlock
            static OPTICAL_CHARACTER_RECOGNITION: Character.UnicodeBlock
            static ENCLOSED_ALPHANUMERICS: Character.UnicodeBlock
            static BOX_DRAWING: Character.UnicodeBlock
            static BLOCK_ELEMENTS: Character.UnicodeBlock
            static GEOMETRIC_SHAPES: Character.UnicodeBlock
            static MISCELLANEOUS_SYMBOLS: Character.UnicodeBlock
            static DINGBATS: Character.UnicodeBlock
            static CJK_SYMBOLS_AND_PUNCTUATION: Character.UnicodeBlock
            static HIRAGANA: Character.UnicodeBlock
            static KATAKANA: Character.UnicodeBlock
            static BOPOMOFO: Character.UnicodeBlock
            static HANGUL_COMPATIBILITY_JAMO: Character.UnicodeBlock
            static KANBUN: Character.UnicodeBlock
            static ENCLOSED_CJK_LETTERS_AND_MONTHS: Character.UnicodeBlock
            static CJK_COMPATIBILITY: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS: Character.UnicodeBlock
            static HANGUL_SYLLABLES: Character.UnicodeBlock
            static PRIVATE_USE_AREA: Character.UnicodeBlock
            static CJK_COMPATIBILITY_IDEOGRAPHS: Character.UnicodeBlock
            static ALPHABETIC_PRESENTATION_FORMS: Character.UnicodeBlock
            static ARABIC_PRESENTATION_FORMS_A: Character.UnicodeBlock
            static COMBINING_HALF_MARKS: Character.UnicodeBlock
            static CJK_COMPATIBILITY_FORMS: Character.UnicodeBlock
            static SMALL_FORM_VARIANTS: Character.UnicodeBlock
            static ARABIC_PRESENTATION_FORMS_B: Character.UnicodeBlock
            static HALFWIDTH_AND_FULLWIDTH_FORMS: Character.UnicodeBlock
            static SPECIALS: Character.UnicodeBlock
            static SURROGATES_AREA: Character.UnicodeBlock
            static SYRIAC: Character.UnicodeBlock
            static THAANA: Character.UnicodeBlock
            static SINHALA: Character.UnicodeBlock
            static MYANMAR: Character.UnicodeBlock
            static ETHIOPIC: Character.UnicodeBlock
            static CHEROKEE: Character.UnicodeBlock
            static UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS: Character.UnicodeBlock
            static OGHAM: Character.UnicodeBlock
            static RUNIC: Character.UnicodeBlock
            static KHMER: Character.UnicodeBlock
            static MONGOLIAN: Character.UnicodeBlock
            static BRAILLE_PATTERNS: Character.UnicodeBlock
            static CJK_RADICALS_SUPPLEMENT: Character.UnicodeBlock
            static KANGXI_RADICALS: Character.UnicodeBlock
            static IDEOGRAPHIC_DESCRIPTION_CHARACTERS: Character.UnicodeBlock
            static BOPOMOFO_EXTENDED: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A: Character.UnicodeBlock
            static YI_SYLLABLES: Character.UnicodeBlock
            static YI_RADICALS: Character.UnicodeBlock
            static CYRILLIC_SUPPLEMENTARY: Character.UnicodeBlock
            static TAGALOG: Character.UnicodeBlock
            static HANUNOO: Character.UnicodeBlock
            static BUHID: Character.UnicodeBlock
            static TAGBANWA: Character.UnicodeBlock
            static LIMBU: Character.UnicodeBlock
            static TAI_LE: Character.UnicodeBlock
            static KHMER_SYMBOLS: Character.UnicodeBlock
            static PHONETIC_EXTENSIONS: Character.UnicodeBlock
            static MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A: Character.UnicodeBlock
            static SUPPLEMENTAL_ARROWS_A: Character.UnicodeBlock
            static SUPPLEMENTAL_ARROWS_B: Character.UnicodeBlock
            static MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B: Character.UnicodeBlock
            static SUPPLEMENTAL_MATHEMATICAL_OPERATORS: Character.UnicodeBlock
            static MISCELLANEOUS_SYMBOLS_AND_ARROWS: Character.UnicodeBlock
            static KATAKANA_PHONETIC_EXTENSIONS: Character.UnicodeBlock
            static YIJING_HEXAGRAM_SYMBOLS: Character.UnicodeBlock
            static VARIATION_SELECTORS: Character.UnicodeBlock
            static LINEAR_B_SYLLABARY: Character.UnicodeBlock
            static LINEAR_B_IDEOGRAMS: Character.UnicodeBlock
            static AEGEAN_NUMBERS: Character.UnicodeBlock
            static OLD_ITALIC: Character.UnicodeBlock
            static GOTHIC: Character.UnicodeBlock
            static UGARITIC: Character.UnicodeBlock
            static DESERET: Character.UnicodeBlock
            static SHAVIAN: Character.UnicodeBlock
            static OSMANYA: Character.UnicodeBlock
            static CYPRIOT_SYLLABARY: Character.UnicodeBlock
            static BYZANTINE_MUSICAL_SYMBOLS: Character.UnicodeBlock
            static MUSICAL_SYMBOLS: Character.UnicodeBlock
            static TAI_XUAN_JING_SYMBOLS: Character.UnicodeBlock
            static MATHEMATICAL_ALPHANUMERIC_SYMBOLS: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B: Character.UnicodeBlock
            static CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT: Character.UnicodeBlock
            static TAGS: Character.UnicodeBlock
            static VARIATION_SELECTORS_SUPPLEMENT: Character.UnicodeBlock
            static SUPPLEMENTARY_PRIVATE_USE_AREA_A: Character.UnicodeBlock
            static SUPPLEMENTARY_PRIVATE_USE_AREA_B: Character.UnicodeBlock
            static HIGH_SURROGATES: Character.UnicodeBlock
            static HIGH_PRIVATE_USE_SURROGATES: Character.UnicodeBlock
            static LOW_SURROGATES: Character.UnicodeBlock
            static ARABIC_SUPPLEMENT: Character.UnicodeBlock
            static NKO: Character.UnicodeBlock
            static SAMARITAN: Character.UnicodeBlock
            static MANDAIC: Character.UnicodeBlock
            static ETHIOPIC_SUPPLEMENT: Character.UnicodeBlock
            static UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED: Character.UnicodeBlock
            static NEW_TAI_LUE: Character.UnicodeBlock
            static BUGINESE: Character.UnicodeBlock
            static TAI_THAM: Character.UnicodeBlock
            static BALINESE: Character.UnicodeBlock
            static SUNDANESE: Character.UnicodeBlock
            static BATAK: Character.UnicodeBlock
            static LEPCHA: Character.UnicodeBlock
            static OL_CHIKI: Character.UnicodeBlock
            static VEDIC_EXTENSIONS: Character.UnicodeBlock
            static PHONETIC_EXTENSIONS_SUPPLEMENT: Character.UnicodeBlock
            static COMBINING_DIACRITICAL_MARKS_SUPPLEMENT: Character.UnicodeBlock
            static GLAGOLITIC: Character.UnicodeBlock
            static LATIN_EXTENDED_C: Character.UnicodeBlock
            static COPTIC: Character.UnicodeBlock
            static GEORGIAN_SUPPLEMENT: Character.UnicodeBlock
            static TIFINAGH: Character.UnicodeBlock
            static ETHIOPIC_EXTENDED: Character.UnicodeBlock
            static CYRILLIC_EXTENDED_A: Character.UnicodeBlock
            static SUPPLEMENTAL_PUNCTUATION: Character.UnicodeBlock
            static CJK_STROKES: Character.UnicodeBlock
            static LISU: Character.UnicodeBlock
            static VAI: Character.UnicodeBlock
            static CYRILLIC_EXTENDED_B: Character.UnicodeBlock
            static BAMUM: Character.UnicodeBlock
            static MODIFIER_TONE_LETTERS: Character.UnicodeBlock
            static LATIN_EXTENDED_D: Character.UnicodeBlock
            static SYLOTI_NAGRI: Character.UnicodeBlock
            static COMMON_INDIC_NUMBER_FORMS: Character.UnicodeBlock
            static PHAGS_PA: Character.UnicodeBlock
            static SAURASHTRA: Character.UnicodeBlock
            static DEVANAGARI_EXTENDED: Character.UnicodeBlock
            static KAYAH_LI: Character.UnicodeBlock
            static REJANG: Character.UnicodeBlock
            static HANGUL_JAMO_EXTENDED_A: Character.UnicodeBlock
            static JAVANESE: Character.UnicodeBlock
            static CHAM: Character.UnicodeBlock
            static MYANMAR_EXTENDED_A: Character.UnicodeBlock
            static TAI_VIET: Character.UnicodeBlock
            static ETHIOPIC_EXTENDED_A: Character.UnicodeBlock
            static MEETEI_MAYEK: Character.UnicodeBlock
            static HANGUL_JAMO_EXTENDED_B: Character.UnicodeBlock
            static VERTICAL_FORMS: Character.UnicodeBlock
            static ANCIENT_GREEK_NUMBERS: Character.UnicodeBlock
            static ANCIENT_SYMBOLS: Character.UnicodeBlock
            static PHAISTOS_DISC: Character.UnicodeBlock
            static LYCIAN: Character.UnicodeBlock
            static CARIAN: Character.UnicodeBlock
            static OLD_PERSIAN: Character.UnicodeBlock
            static IMPERIAL_ARAMAIC: Character.UnicodeBlock
            static PHOENICIAN: Character.UnicodeBlock
            static LYDIAN: Character.UnicodeBlock
            static KHAROSHTHI: Character.UnicodeBlock
            static OLD_SOUTH_ARABIAN: Character.UnicodeBlock
            static AVESTAN: Character.UnicodeBlock
            static INSCRIPTIONAL_PARTHIAN: Character.UnicodeBlock
            static INSCRIPTIONAL_PAHLAVI: Character.UnicodeBlock
            static OLD_TURKIC: Character.UnicodeBlock
            static RUMI_NUMERAL_SYMBOLS: Character.UnicodeBlock
            static BRAHMI: Character.UnicodeBlock
            static KAITHI: Character.UnicodeBlock
            static CUNEIFORM: Character.UnicodeBlock
            static CUNEIFORM_NUMBERS_AND_PUNCTUATION: Character.UnicodeBlock
            static EGYPTIAN_HIEROGLYPHS: Character.UnicodeBlock
            static BAMUM_SUPPLEMENT: Character.UnicodeBlock
            static KANA_SUPPLEMENT: Character.UnicodeBlock
            static ANCIENT_GREEK_MUSICAL_NOTATION: Character.UnicodeBlock
            static COUNTING_ROD_NUMERALS: Character.UnicodeBlock
            static MAHJONG_TILES: Character.UnicodeBlock
            static DOMINO_TILES: Character.UnicodeBlock
            static PLAYING_CARDS: Character.UnicodeBlock
            static ENCLOSED_ALPHANUMERIC_SUPPLEMENT: Character.UnicodeBlock
            static ENCLOSED_IDEOGRAPHIC_SUPPLEMENT: Character.UnicodeBlock
            static MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS: Character.UnicodeBlock
            static EMOTICONS: Character.UnicodeBlock
            static TRANSPORT_AND_MAP_SYMBOLS: Character.UnicodeBlock
            static ALCHEMICAL_SYMBOLS: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D: Character.UnicodeBlock
            static ARABIC_EXTENDED_A: Character.UnicodeBlock
            static SUNDANESE_SUPPLEMENT: Character.UnicodeBlock
            static MEETEI_MAYEK_EXTENSIONS: Character.UnicodeBlock
            static MEROITIC_HIEROGLYPHS: Character.UnicodeBlock
            static MEROITIC_CURSIVE: Character.UnicodeBlock
            static SORA_SOMPENG: Character.UnicodeBlock
            static CHAKMA: Character.UnicodeBlock
            static SHARADA: Character.UnicodeBlock
            static TAKRI: Character.UnicodeBlock
            static MIAO: Character.UnicodeBlock
            static ARABIC_MATHEMATICAL_ALPHABETIC_SYMBOLS: Character.UnicodeBlock
            static COMBINING_DIACRITICAL_MARKS_EXTENDED: Character.UnicodeBlock
            static MYANMAR_EXTENDED_B: Character.UnicodeBlock
            static LATIN_EXTENDED_E: Character.UnicodeBlock
            static COPTIC_EPACT_NUMBERS: Character.UnicodeBlock
            static OLD_PERMIC: Character.UnicodeBlock
            static ELBASAN: Character.UnicodeBlock
            static CAUCASIAN_ALBANIAN: Character.UnicodeBlock
            static LINEAR_A: Character.UnicodeBlock
            static PALMYRENE: Character.UnicodeBlock
            static NABATAEAN: Character.UnicodeBlock
            static OLD_NORTH_ARABIAN: Character.UnicodeBlock
            static MANICHAEAN: Character.UnicodeBlock
            static PSALTER_PAHLAVI: Character.UnicodeBlock
            static MAHAJANI: Character.UnicodeBlock
            static SINHALA_ARCHAIC_NUMBERS: Character.UnicodeBlock
            static KHOJKI: Character.UnicodeBlock
            static KHUDAWADI: Character.UnicodeBlock
            static GRANTHA: Character.UnicodeBlock
            static TIRHUTA: Character.UnicodeBlock
            static SIDDHAM: Character.UnicodeBlock
            static MODI: Character.UnicodeBlock
            static WARANG_CITI: Character.UnicodeBlock
            static PAU_CIN_HAU: Character.UnicodeBlock
            static MRO: Character.UnicodeBlock
            static BASSA_VAH: Character.UnicodeBlock
            static PAHAWH_HMONG: Character.UnicodeBlock
            static DUPLOYAN: Character.UnicodeBlock
            static SHORTHAND_FORMAT_CONTROLS: Character.UnicodeBlock
            static MENDE_KIKAKUI: Character.UnicodeBlock
            static ORNAMENTAL_DINGBATS: Character.UnicodeBlock
            static GEOMETRIC_SHAPES_EXTENDED: Character.UnicodeBlock
            static SUPPLEMENTAL_ARROWS_C: Character.UnicodeBlock
            static CHEROKEE_SUPPLEMENT: Character.UnicodeBlock
            static HATRAN: Character.UnicodeBlock
            static OLD_HUNGARIAN: Character.UnicodeBlock
            static MULTANI: Character.UnicodeBlock
            static AHOM: Character.UnicodeBlock
            static EARLY_DYNASTIC_CUNEIFORM: Character.UnicodeBlock
            static ANATOLIAN_HIEROGLYPHS: Character.UnicodeBlock
            static SUTTON_SIGNWRITING: Character.UnicodeBlock
            static SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E: Character.UnicodeBlock
            static SYRIAC_SUPPLEMENT: Character.UnicodeBlock
            static CYRILLIC_EXTENDED_C: Character.UnicodeBlock
            static OSAGE: Character.UnicodeBlock
            static NEWA: Character.UnicodeBlock
            static MONGOLIAN_SUPPLEMENT: Character.UnicodeBlock
            static MARCHEN: Character.UnicodeBlock
            static IDEOGRAPHIC_SYMBOLS_AND_PUNCTUATION: Character.UnicodeBlock
            static TANGUT: Character.UnicodeBlock
            static TANGUT_COMPONENTS: Character.UnicodeBlock
            static KANA_EXTENDED_A: Character.UnicodeBlock
            static GLAGOLITIC_SUPPLEMENT: Character.UnicodeBlock
            static ADLAM: Character.UnicodeBlock
            static MASARAM_GONDI: Character.UnicodeBlock
            static ZANABAZAR_SQUARE: Character.UnicodeBlock
            static NUSHU: Character.UnicodeBlock
            static SOYOMBO: Character.UnicodeBlock
            static BHAIKSUKI: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F: Character.UnicodeBlock
            static GEORGIAN_EXTENDED: Character.UnicodeBlock
            static HANIFI_ROHINGYA: Character.UnicodeBlock
            static OLD_SOGDIAN: Character.UnicodeBlock
            static SOGDIAN: Character.UnicodeBlock
            static DOGRA: Character.UnicodeBlock
            static GUNJALA_GONDI: Character.UnicodeBlock
            static MAKASAR: Character.UnicodeBlock
            static MEDEFAIDRIN: Character.UnicodeBlock
            static MAYAN_NUMERALS: Character.UnicodeBlock
            static INDIC_SIYAQ_NUMBERS: Character.UnicodeBlock
            static CHESS_SYMBOLS: Character.UnicodeBlock
            static ELYMAIC: Character.UnicodeBlock
            static NANDINAGARI: Character.UnicodeBlock
            static TAMIL_SUPPLEMENT: Character.UnicodeBlock
            static EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS: Character.UnicodeBlock
            static SMALL_KANA_EXTENSION: Character.UnicodeBlock
            static NYIAKENG_PUACHUE_HMONG: Character.UnicodeBlock
            static WANCHO: Character.UnicodeBlock
            static OTTOMAN_SIYAQ_NUMBERS: Character.UnicodeBlock
            static SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A: Character.UnicodeBlock
            static YEZIDI: Character.UnicodeBlock
            static CHORASMIAN: Character.UnicodeBlock
            static DIVES_AKURU: Character.UnicodeBlock
            static LISU_SUPPLEMENT: Character.UnicodeBlock
            static KHITAN_SMALL_SCRIPT: Character.UnicodeBlock
            static TANGUT_SUPPLEMENT: Character.UnicodeBlock
            static SYMBOLS_FOR_LEGACY_COMPUTING: Character.UnicodeBlock
            static CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G: Character.UnicodeBlock

            static of(arg0: String): Character.UnicodeBlock;

            static of(arg0: number): Character.UnicodeBlock;

            static forName(arg0: String): Character.UnicodeBlock;
        }

        export class UnicodeScript extends Enum<Character.UnicodeScript> {
            static COMMON: Character.UnicodeScript
            static LATIN: Character.UnicodeScript
            static GREEK: Character.UnicodeScript
            static CYRILLIC: Character.UnicodeScript
            static ARMENIAN: Character.UnicodeScript
            static HEBREW: Character.UnicodeScript
            static ARABIC: Character.UnicodeScript
            static SYRIAC: Character.UnicodeScript
            static THAANA: Character.UnicodeScript
            static DEVANAGARI: Character.UnicodeScript
            static BENGALI: Character.UnicodeScript
            static GURMUKHI: Character.UnicodeScript
            static GUJARATI: Character.UnicodeScript
            static ORIYA: Character.UnicodeScript
            static TAMIL: Character.UnicodeScript
            static TELUGU: Character.UnicodeScript
            static KANNADA: Character.UnicodeScript
            static MALAYALAM: Character.UnicodeScript
            static SINHALA: Character.UnicodeScript
            static THAI: Character.UnicodeScript
            static LAO: Character.UnicodeScript
            static TIBETAN: Character.UnicodeScript
            static MYANMAR: Character.UnicodeScript
            static GEORGIAN: Character.UnicodeScript
            static HANGUL: Character.UnicodeScript
            static ETHIOPIC: Character.UnicodeScript
            static CHEROKEE: Character.UnicodeScript
            static CANADIAN_ABORIGINAL: Character.UnicodeScript
            static OGHAM: Character.UnicodeScript
            static RUNIC: Character.UnicodeScript
            static KHMER: Character.UnicodeScript
            static MONGOLIAN: Character.UnicodeScript
            static HIRAGANA: Character.UnicodeScript
            static KATAKANA: Character.UnicodeScript
            static BOPOMOFO: Character.UnicodeScript
            static HAN: Character.UnicodeScript
            static YI: Character.UnicodeScript
            static OLD_ITALIC: Character.UnicodeScript
            static GOTHIC: Character.UnicodeScript
            static DESERET: Character.UnicodeScript
            static INHERITED: Character.UnicodeScript
            static TAGALOG: Character.UnicodeScript
            static HANUNOO: Character.UnicodeScript
            static BUHID: Character.UnicodeScript
            static TAGBANWA: Character.UnicodeScript
            static LIMBU: Character.UnicodeScript
            static TAI_LE: Character.UnicodeScript
            static LINEAR_B: Character.UnicodeScript
            static UGARITIC: Character.UnicodeScript
            static SHAVIAN: Character.UnicodeScript
            static OSMANYA: Character.UnicodeScript
            static CYPRIOT: Character.UnicodeScript
            static BRAILLE: Character.UnicodeScript
            static BUGINESE: Character.UnicodeScript
            static COPTIC: Character.UnicodeScript
            static NEW_TAI_LUE: Character.UnicodeScript
            static GLAGOLITIC: Character.UnicodeScript
            static TIFINAGH: Character.UnicodeScript
            static SYLOTI_NAGRI: Character.UnicodeScript
            static OLD_PERSIAN: Character.UnicodeScript
            static KHAROSHTHI: Character.UnicodeScript
            static BALINESE: Character.UnicodeScript
            static CUNEIFORM: Character.UnicodeScript
            static PHOENICIAN: Character.UnicodeScript
            static PHAGS_PA: Character.UnicodeScript
            static NKO: Character.UnicodeScript
            static SUNDANESE: Character.UnicodeScript
            static BATAK: Character.UnicodeScript
            static LEPCHA: Character.UnicodeScript
            static OL_CHIKI: Character.UnicodeScript
            static VAI: Character.UnicodeScript
            static SAURASHTRA: Character.UnicodeScript
            static KAYAH_LI: Character.UnicodeScript
            static REJANG: Character.UnicodeScript
            static LYCIAN: Character.UnicodeScript
            static CARIAN: Character.UnicodeScript
            static LYDIAN: Character.UnicodeScript
            static CHAM: Character.UnicodeScript
            static TAI_THAM: Character.UnicodeScript
            static TAI_VIET: Character.UnicodeScript
            static AVESTAN: Character.UnicodeScript
            static EGYPTIAN_HIEROGLYPHS: Character.UnicodeScript
            static SAMARITAN: Character.UnicodeScript
            static MANDAIC: Character.UnicodeScript
            static LISU: Character.UnicodeScript
            static BAMUM: Character.UnicodeScript
            static JAVANESE: Character.UnicodeScript
            static MEETEI_MAYEK: Character.UnicodeScript
            static IMPERIAL_ARAMAIC: Character.UnicodeScript
            static OLD_SOUTH_ARABIAN: Character.UnicodeScript
            static INSCRIPTIONAL_PARTHIAN: Character.UnicodeScript
            static INSCRIPTIONAL_PAHLAVI: Character.UnicodeScript
            static OLD_TURKIC: Character.UnicodeScript
            static BRAHMI: Character.UnicodeScript
            static KAITHI: Character.UnicodeScript
            static MEROITIC_HIEROGLYPHS: Character.UnicodeScript
            static MEROITIC_CURSIVE: Character.UnicodeScript
            static SORA_SOMPENG: Character.UnicodeScript
            static CHAKMA: Character.UnicodeScript
            static SHARADA: Character.UnicodeScript
            static TAKRI: Character.UnicodeScript
            static MIAO: Character.UnicodeScript
            static CAUCASIAN_ALBANIAN: Character.UnicodeScript
            static BASSA_VAH: Character.UnicodeScript
            static DUPLOYAN: Character.UnicodeScript
            static ELBASAN: Character.UnicodeScript
            static GRANTHA: Character.UnicodeScript
            static PAHAWH_HMONG: Character.UnicodeScript
            static KHOJKI: Character.UnicodeScript
            static LINEAR_A: Character.UnicodeScript
            static MAHAJANI: Character.UnicodeScript
            static MANICHAEAN: Character.UnicodeScript
            static MENDE_KIKAKUI: Character.UnicodeScript
            static MODI: Character.UnicodeScript
            static MRO: Character.UnicodeScript
            static OLD_NORTH_ARABIAN: Character.UnicodeScript
            static NABATAEAN: Character.UnicodeScript
            static PALMYRENE: Character.UnicodeScript
            static PAU_CIN_HAU: Character.UnicodeScript
            static OLD_PERMIC: Character.UnicodeScript
            static PSALTER_PAHLAVI: Character.UnicodeScript
            static SIDDHAM: Character.UnicodeScript
            static KHUDAWADI: Character.UnicodeScript
            static TIRHUTA: Character.UnicodeScript
            static WARANG_CITI: Character.UnicodeScript
            static AHOM: Character.UnicodeScript
            static ANATOLIAN_HIEROGLYPHS: Character.UnicodeScript
            static HATRAN: Character.UnicodeScript
            static MULTANI: Character.UnicodeScript
            static OLD_HUNGARIAN: Character.UnicodeScript
            static SIGNWRITING: Character.UnicodeScript
            static ADLAM: Character.UnicodeScript
            static BHAIKSUKI: Character.UnicodeScript
            static MARCHEN: Character.UnicodeScript
            static NEWA: Character.UnicodeScript
            static OSAGE: Character.UnicodeScript
            static TANGUT: Character.UnicodeScript
            static MASARAM_GONDI: Character.UnicodeScript
            static NUSHU: Character.UnicodeScript
            static SOYOMBO: Character.UnicodeScript
            static ZANABAZAR_SQUARE: Character.UnicodeScript
            static HANIFI_ROHINGYA: Character.UnicodeScript
            static OLD_SOGDIAN: Character.UnicodeScript
            static SOGDIAN: Character.UnicodeScript
            static DOGRA: Character.UnicodeScript
            static GUNJALA_GONDI: Character.UnicodeScript
            static MAKASAR: Character.UnicodeScript
            static MEDEFAIDRIN: Character.UnicodeScript
            static ELYMAIC: Character.UnicodeScript
            static NANDINAGARI: Character.UnicodeScript
            static NYIAKENG_PUACHUE_HMONG: Character.UnicodeScript
            static WANCHO: Character.UnicodeScript
            static YEZIDI: Character.UnicodeScript
            static CHORASMIAN: Character.UnicodeScript
            static DIVES_AKURU: Character.UnicodeScript
            static KHITAN_SMALL_SCRIPT: Character.UnicodeScript
            static UNKNOWN: Character.UnicodeScript

            static values(): Character.UnicodeScript[];

            static valueOf(arg0: String): Character.UnicodeScript;

            static of(arg0: number): Character.UnicodeScript;

            static forName(arg0: String): Character.UnicodeScript;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class Class<T extends Object> extends Object implements Serializable, GenericDeclaration, Type, AnnotatedElement, TypeDescriptor.OfField<Class<any>>, Constable {
        toString(): string;

        toGenericString(): String;

        static forName(arg0: String): Class<any>;

        static forName(arg0: String, arg1: boolean, arg2: ClassLoader): Class<any>;

        static forName(arg0: Module, arg1: String): Class<any>;

        newInstance(): T;

        isInstance(arg0: Object): boolean;

        isAssignableFrom(arg0: Class<any>): boolean;

        isInterface(): boolean;

        isArray(): boolean;

        isPrimitive(): boolean;

        isAnnotation(): boolean;

        isSynthetic(): boolean;

        getName(): String;

        getClassLoader(): ClassLoader;

        getModule(): Module;

        getTypeParameters(): TypeVariable<Class<T>>[];

        getSuperclass(): Class<T>;

        getGenericSuperclass(): Type;

        getPackage(): Package;

        getPackageName(): String;

        getInterfaces(): Class<any>[];

        getGenericInterfaces(): Type[];

        getComponentType(): Class<any>;

        getModifiers(): number;

        getSigners(): Object[];

        getEnclosingMethod(): Method;

        getEnclosingConstructor(): Constructor<any>;

        getDeclaringClass(): Class<any>;

        getEnclosingClass(): Class<any>;

        getSimpleName(): String;

        getTypeName(): String;

        getCanonicalName(): String;

        isAnonymousClass(): boolean;

        isLocalClass(): boolean;

        isMemberClass(): boolean;

        getClasses(): Class<any>[];

        getFields(): Field[];

        getMethods(): Method[];

        getConstructors(): Constructor<any>[];

        getField(arg0: String): Field;

        getMethod(arg0: String, arg1: Class<any>[]): Method;

        getConstructor(arg0: Class<any>[]): Constructor<T>;

        getDeclaredClasses(): Class<any>[];

        getDeclaredFields(): Field[];

        getRecordComponents(): RecordComponent[];

        getDeclaredMethods(): Method[];

        getDeclaredConstructors(): Constructor<any>[];

        getDeclaredField(arg0: String): Field;

        getDeclaredMethod(arg0: String, arg1: Class<any>[]): Method;

        getDeclaredConstructor(arg0: Class<any>[]): Constructor<T>;

        getResourceAsStream(arg0: String): InputStream;

        getResource(arg0: String): URL;

        getProtectionDomain(): ProtectionDomain;

        desiredAssertionStatus(): boolean;

        isEnum(): boolean;

        isRecord(): boolean;

        getEnumConstants(): T[];

        cast(arg0: Object): T;

        asSubclass<U extends Object>(arg0: Class<U>): Class<U>;

        getAnnotation<A extends Annotation>(arg0: Class<A>): A;

        isAnnotationPresent(arg0: Class<Annotation>): boolean;

        getAnnotationsByType<A extends Annotation>(arg0: Class<A>): A[];

        getAnnotations(): Annotation[];

        getDeclaredAnnotation<A extends Annotation>(arg0: Class<A>): A;

        getDeclaredAnnotationsByType<A extends Annotation>(arg0: Class<A>): A[];

        getDeclaredAnnotations(): Annotation[];

        getAnnotatedSuperclass(): AnnotatedType;

        getAnnotatedInterfaces(): AnnotatedType[];

        getNestHost(): Class<any>;

        isNestmateOf(arg0: Class<any>): boolean;

        getNestMembers(): Class<any>[];

        descriptorString(): String;

        componentType(): Class<any>;

        arrayType(): Class<any>;

        describeConstable(): Optional<ClassDesc>;

        isHidden(): boolean;

        getPermittedSubclasses(): Class<any>[];

        isSealed(): boolean;
    }

    export class ClassCastException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class ClassCircularityError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export class ClassFormatError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export abstract class ClassLoader {

        getName(): String;

        loadClass(arg0: String): Class<any>;

        getResource(arg0: String): URL;

        getResources(arg0: String): Enumeration<URL>;

        resources(arg0: String): Stream<URL>;

        isRegisteredAsParallelCapable(): boolean;

        static getSystemResource(arg0: String): URL;

        static getSystemResources(arg0: String): Enumeration<URL>;

        getResourceAsStream(arg0: String): InputStream;

        static getSystemResourceAsStream(arg0: String): InputStream;

        getParent(): ClassLoader;

        getUnnamedModule(): Module;

        static getPlatformClassLoader(): ClassLoader;

        static getSystemClassLoader(): ClassLoader;

        getDefinedPackage(arg0: String): Package;

        getDefinedPackages(): Package[];

        setDefaultAssertionStatus(arg0: boolean): void;

        setPackageAssertionStatus(arg0: String, arg1: boolean): void;

        setClassAssertionStatus(arg0: String, arg1: boolean): void;

        clearAssertionStatus(): void;
    }

    export class ClassNotFoundException extends ReflectiveOperationException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);

        getException(): Throwable;
    }

    export abstract class ClassValue<T extends Object> extends Object {

        get(arg0: Class<any>): T;

        remove(arg0: Class<any>): void;
    }

    export class CloneNotSupportedException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface Cloneable {
    }

    export interface Comparable<T extends Object> extends Object {

        compareTo(arg0: T): number;
    }

    export class Compiler {

        static compileClass(arg0: Class<any>): boolean;

        static compileClasses(arg0: String): boolean;

        static command(arg0: Object): Object;

        static enable(): void;

        static disable(): void;
    }


    export class Double extends Number implements Comparable<Number>, Constable, ConstantDesc {
        static POSITIVE_INFINITY: number
        static NEGATIVE_INFINITY: number
        static NaN: number
        static MAX_VALUE: number
        static MIN_NORMAL: number
        static MIN_VALUE: number
        static MAX_EXPONENT: number
        static MIN_EXPONENT: number
        static SIZE: number
        static BYTES: number
        static TYPE: Class<Number>
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number): String;

        static toHexString(arg0: number): String;

        static valueOf(arg0: String): Number;

        static valueOf(arg0: number): Number;

        static parseDouble(arg0: String): number;

        static isNaN(arg0: number): boolean;

        static isInfinite(arg0: number): boolean;

        static isFinite(arg0: number): boolean;

        isNaN(): boolean;

        isInfinite(): boolean;
        toString(): string;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        static doubleToLongBits(arg0: number): number;

        static doubleToRawLongBits(arg0: number): number;

        static longBitsToDouble(arg0: number): number;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static sum(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        describeConstable(): Optional<Number>;

        resolveConstantDesc(arg0: Lookup): Number;
    }

    export abstract class Enum<E extends Enum<E>> extends Object implements Constable, Comparable<E>, Serializable {

        name(): String;

        ordinal(): number;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;

        compareTo(arg0: E): number;

        getDeclaringClass(): Class<E>;

        describeConstable(): Optional<Enum.EnumDesc<E>>;

        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }
    export namespace Enum {
        export class EnumDesc<E extends Enum<E>> extends DynamicConstantDesc<E> {

            static of<E extends Enum<E>>(arg0: ClassDesc, arg1: String): Enum.EnumDesc<E>;

            resolveConstantDesc(arg0: Lookup): E;
            toString(): string;
        }

    }

    export class EnumConstantNotPresentException extends RuntimeException {
        constructor(arg0: Class<Enum>, arg1: String);

        enumType(): Class<Enum>;

        constantName(): String;
    }

    export class Error extends Throwable {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class Exception extends Throwable {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class ExceptionInInitializerError extends LinkageError {
        constructor();
        constructor(arg0: Throwable);
        constructor(arg0: String);

        getException(): Throwable;
    }

    export class Float extends Number implements Comparable<Number>, Constable, ConstantDesc {
        static POSITIVE_INFINITY: number
        static NEGATIVE_INFINITY: number
        static NaN: number
        static MAX_VALUE: number
        static MIN_NORMAL: number
        static MIN_VALUE: number
        static MAX_EXPONENT: number
        static MIN_EXPONENT: number
        static SIZE: number
        static BYTES: number
        static TYPE: Class<Number>
        constructor(arg0: number);
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number): String;

        static toHexString(arg0: number): String;

        static valueOf(arg0: String): Number;

        static valueOf(arg0: number): Number;

        static parseFloat(arg0: String): number;

        static isNaN(arg0: number): boolean;

        static isInfinite(arg0: number): boolean;

        static isFinite(arg0: number): boolean;

        isNaN(): boolean;

        isInfinite(): boolean;
        toString(): string;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        static floatToIntBits(arg0: number): number;

        static floatToRawIntBits(arg0: number): number;

        static intBitsToFloat(arg0: number): number;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static sum(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        describeConstable(): Optional<Number>;

        resolveConstantDesc(arg0: Lookup): Number;
    }


    export class IllegalAccessError extends IncompatibleClassChangeError {
        constructor();
        constructor(arg0: String);
    }

    export class IllegalAccessException extends ReflectiveOperationException {
        constructor();
        constructor(arg0: String);
    }

    export class IllegalArgumentException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class IllegalCallerException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class IllegalMonitorStateException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class IllegalStateException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class IllegalThreadStateException extends IllegalArgumentException {
        constructor();
        constructor(arg0: String);
    }

    export class IncompatibleClassChangeError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export class IndexOutOfBoundsException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: number);
        constructor(arg0: number);
    }

    export class InheritableThreadLocal<T extends Object> extends ThreadLocal<T> {
        constructor();
    }

    export class InstantiationError extends IncompatibleClassChangeError {
        constructor();
        constructor(arg0: String);
    }

    export class InstantiationException extends ReflectiveOperationException {
        constructor();
        constructor(arg0: String);
    }

    export class Integer extends Number implements Comparable<Number>, Constable, ConstantDesc {
        static MIN_VALUE: number
        static MAX_VALUE: number
        static TYPE: Class<Number>
        static SIZE: number
        static BYTES: number
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number, arg1: number): String;

        static toUnsignedString(arg0: number, arg1: number): String;

        static toHexString(arg0: number): String;

        static toOctalString(arg0: number): String;

        static toBinaryString(arg0: number): String;

        static toString(arg0: number): String;

        static toUnsignedString(arg0: number): String;

        static parseInt(arg0: String, arg1: number): number;

        static parseInt(arg0: CharSequence, arg1: number, arg2: number, arg3: number): number;

        static parseInt(arg0: String): number;

        static parseUnsignedInt(arg0: String, arg1: number): number;

        static parseUnsignedInt(arg0: CharSequence, arg1: number, arg2: number, arg3: number): number;

        static parseUnsignedInt(arg0: String): number;

        static valueOf(arg0: String, arg1: number): Number;

        static valueOf(arg0: String): Number;

        static valueOf(arg0: number): Number;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;
        toString(): string;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        static getInteger(arg0: String): Number;

        static getInteger(arg0: String, arg1: number): Number;

        static getInteger(arg0: String, arg1: Number): Number;

        static decode(arg0: String): Number;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static compareUnsigned(arg0: number, arg1: number): number;

        static toUnsignedLong(arg0: number): number;

        static divideUnsigned(arg0: number, arg1: number): number;

        static remainderUnsigned(arg0: number, arg1: number): number;

        static highestOneBit(arg0: number): number;

        static lowestOneBit(arg0: number): number;

        static numberOfLeadingZeros(arg0: number): number;

        static numberOfTrailingZeros(arg0: number): number;

        static bitCount(arg0: number): number;

        static rotateLeft(arg0: number, arg1: number): number;

        static rotateRight(arg0: number, arg1: number): number;

        static reverse(arg0: number): number;

        static signum(arg0: number): number;

        static reverseBytes(arg0: number): number;

        static sum(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        describeConstable(): Optional<Number>;

        resolveConstantDesc(arg0: Lookup): Number;
    }

    export class InternalError extends VirtualMachineError {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class InterruptedException extends Exception {
        constructor();
        constructor(arg0: String);
    }

    export interface Iterable<T extends Object> extends Object, globalThis.Array<T> {

        iterator(): Iterator<T>;

/* default */ forEach(arg0: Consumer<T>): void;

/* default */ spliterator(): Spliterator<T>;
        [Symbol.iterator](): globalThis.Iterator<T>;
    }

    export class LayerInstantiationException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: Throwable);
        constructor(arg0: String, arg1: Throwable);
    }

    export class LinkageError extends Error {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
    }

    export class Long extends Number implements Comparable<Number>, Constable, ConstantDesc {
        static MIN_VALUE: number
        static MAX_VALUE: number
        static TYPE: Class<Number>
        static SIZE: number
        static BYTES: number
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number, arg1: number): String;

        static toUnsignedString(arg0: number, arg1: number): String;

        static toHexString(arg0: number): String;

        static toOctalString(arg0: number): String;

        static toBinaryString(arg0: number): String;

        static toString(arg0: number): String;

        static toUnsignedString(arg0: number): String;

        static parseLong(arg0: String, arg1: number): number;

        static parseLong(arg0: CharSequence, arg1: number, arg2: number, arg3: number): number;

        static parseLong(arg0: String): number;

        static parseUnsignedLong(arg0: String, arg1: number): number;

        static parseUnsignedLong(arg0: CharSequence, arg1: number, arg2: number, arg3: number): number;

        static parseUnsignedLong(arg0: String): number;

        static valueOf(arg0: String, arg1: number): Number;

        static valueOf(arg0: String): Number;

        static valueOf(arg0: number): Number;

        static decode(arg0: String): Number;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;
        toString(): string;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        static getLong(arg0: String): Number;

        static getLong(arg0: String, arg1: number): Number;

        static getLong(arg0: String, arg1: Number): Number;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static compareUnsigned(arg0: number, arg1: number): number;

        static divideUnsigned(arg0: number, arg1: number): number;

        static remainderUnsigned(arg0: number, arg1: number): number;

        static highestOneBit(arg0: number): number;

        static lowestOneBit(arg0: number): number;

        static numberOfLeadingZeros(arg0: number): number;

        static numberOfTrailingZeros(arg0: number): number;

        static bitCount(arg0: number): number;

        static rotateLeft(arg0: number, arg1: number): number;

        static rotateRight(arg0: number, arg1: number): number;

        static reverse(arg0: number): number;

        static signum(arg0: number): number;

        static reverseBytes(arg0: number): number;

        static sum(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        describeConstable(): Optional<Number>;

        resolveConstantDesc(arg0: Lookup): Number;
    }

    export class Math {
        static E: number
        static PI: number

        static sin(arg0: number): number;

        static cos(arg0: number): number;

        static tan(arg0: number): number;

        static asin(arg0: number): number;

        static acos(arg0: number): number;

        static atan(arg0: number): number;

        static toRadians(arg0: number): number;

        static toDegrees(arg0: number): number;

        static exp(arg0: number): number;

        static log(arg0: number): number;

        static log10(arg0: number): number;

        static sqrt(arg0: number): number;

        static cbrt(arg0: number): number;

        static IEEEremainder(arg0: number, arg1: number): number;

        static ceil(arg0: number): number;

        static floor(arg0: number): number;

        static rint(arg0: number): number;

        static atan2(arg0: number, arg1: number): number;

        static pow(arg0: number, arg1: number): number;

        static round(arg0: number): number;

        static round(arg0: number): number;

        static random(): number;

        static addExact(arg0: number, arg1: number): number;

        static addExact(arg0: number, arg1: number): number;

        static subtractExact(arg0: number, arg1: number): number;

        static subtractExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static incrementExact(arg0: number): number;

        static incrementExact(arg0: number): number;

        static decrementExact(arg0: number): number;

        static decrementExact(arg0: number): number;

        static negateExact(arg0: number): number;

        static negateExact(arg0: number): number;

        static toIntExact(arg0: number): number;

        static multiplyFull(arg0: number, arg1: number): number;

        static multiplyHigh(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static abs(arg0: number): number;

        static absExact(arg0: number): number;

        static abs(arg0: number): number;

        static absExact(arg0: number): number;

        static abs(arg0: number): number;

        static abs(arg0: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static fma(arg0: number, arg1: number, arg2: number): number;

        static fma(arg0: number, arg1: number, arg2: number): number;

        static ulp(arg0: number): number;

        static ulp(arg0: number): number;

        static signum(arg0: number): number;

        static signum(arg0: number): number;

        static sinh(arg0: number): number;

        static cosh(arg0: number): number;

        static tanh(arg0: number): number;

        static hypot(arg0: number, arg1: number): number;

        static expm1(arg0: number): number;

        static log1p(arg0: number): number;

        static copySign(arg0: number, arg1: number): number;

        static copySign(arg0: number, arg1: number): number;

        static getExponent(arg0: number): number;

        static getExponent(arg0: number): number;

        static nextAfter(arg0: number, arg1: number): number;

        static nextAfter(arg0: number, arg1: number): number;

        static nextUp(arg0: number): number;

        static nextUp(arg0: number): number;

        static nextDown(arg0: number): number;

        static nextDown(arg0: number): number;

        static scalb(arg0: number, arg1: number): number;

        static scalb(arg0: number, arg1: number): number;
    }

    export interface Module extends AnnotatedElement { }
    export class Module implements AnnotatedElement {

        isNamed(): boolean;

        getName(): String;

        getClassLoader(): ClassLoader;

        getDescriptor(): ModuleDescriptor;

        getLayer(): ModuleLayer;

        canRead(arg0: Module): boolean;

        addReads(arg0: Module): Module;

        isExported(arg0: String, arg1: Module): boolean;

        isOpen(arg0: String, arg1: Module): boolean;

        isExported(arg0: String): boolean;

        isOpen(arg0: String): boolean;

        addExports(arg0: String, arg1: Module): Module;

        addOpens(arg0: String, arg1: Module): Module;

        addUses(arg0: Class<any>): Module;

        canUse(arg0: Class<any>): boolean;

        getPackages(): Set<String>;

        getAnnotation<T extends Annotation>(arg0: Class<T>): T;

        getAnnotations(): Annotation[];

        getDeclaredAnnotations(): Annotation[];

        getResourceAsStream(arg0: String): InputStream;
        toString(): string;
    }

    export class ModuleLayer {

        defineModulesWithOneLoader(arg0: Configuration, arg1: ClassLoader): ModuleLayer;

        defineModulesWithManyLoaders(arg0: Configuration, arg1: ClassLoader): ModuleLayer;

        defineModules(arg0: Configuration, arg1: Function<String, ClassLoader>): ModuleLayer;

        static defineModulesWithOneLoader(arg0: Configuration, arg1: List<ModuleLayer>, arg2: ClassLoader): ModuleLayer.Controller;

        static defineModulesWithManyLoaders(arg0: Configuration, arg1: List<ModuleLayer>, arg2: ClassLoader): ModuleLayer.Controller;

        static defineModules(arg0: Configuration, arg1: List<ModuleLayer>, arg2: Function<String, ClassLoader>): ModuleLayer.Controller;

        configuration(): Configuration;

        parents(): List<ModuleLayer>;

        modules(): Set<Module>;

        findModule(arg0: String): Optional<Module>;

        findLoader(arg0: String): ClassLoader;
        toString(): string;

        static empty(): ModuleLayer;

        static boot(): ModuleLayer;
    }
    export namespace ModuleLayer {
        export class Controller {

            layer(): ModuleLayer;

            addReads(arg0: Module, arg1: Module): ModuleLayer.Controller;

            addExports(arg0: Module, arg1: String, arg2: Module): ModuleLayer.Controller;

            addOpens(arg0: Module, arg1: String, arg2: Module): ModuleLayer.Controller;
        }

    }

    export class NegativeArraySizeException extends RuntimeException {
        constructor();
        constructor(arg0: String);
    }

    export class NoClassDefFoundError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export class NoSuchFieldError extends IncompatibleClassChangeError {
        constructor();
        constructor(arg0: String);
    }

    export class NoSuchFieldException extends ReflectiveOperationException {
        constructor();
        constructor(arg0: String);
    }

    export class NoSuchMethodError extends IncompatibleClassChangeError {
        constructor();
        constructor(arg0: String);
    }

    export class NoSuchMethodException extends ReflectiveOperationException {
        constructor();
        constructor(arg0: String);
    }

    export class NullPointerException extends RuntimeException {
        constructor();
        constructor(arg0: String);

        fillInStackTrace(): Throwable;

        getMessage(): String;
    }

    export abstract class Number implements Serializable {
        constructor();

        abstract intValue(): number;

        abstract longValue(): number;

        abstract floatValue(): number;

        abstract doubleValue(): number;

        byteValue(): number;

        shortValue(): number;
    }

    export class NumberFormatException extends IllegalArgumentException {
        constructor();
        constructor(arg0: String);
    }

    export class OutOfMemoryError extends VirtualMachineError {
        constructor();
        constructor(arg0: String);
    }


    export class Package extends NamedPackage implements AnnotatedElement {

        getName(): String;

        getSpecificationTitle(): String;

        getSpecificationVersion(): String;

        getSpecificationVendor(): String;

        getImplementationTitle(): String;

        getImplementationVersion(): String;

        getImplementationVendor(): String;

        isSealed(): boolean;

        isSealed(arg0: URL): boolean;

        isCompatibleWith(arg0: String): boolean;

        static getPackage(arg0: String): Package;

        static getPackages(): Package[];

        hashCode(): number;
        toString(): string;

        getAnnotation<A extends Annotation>(arg0: Class<A>): A;

        isAnnotationPresent(arg0: Class<Annotation>): boolean;

        getAnnotationsByType<A extends Annotation>(arg0: Class<A>): A[];

        getAnnotations(): Annotation[];

        getDeclaredAnnotation<A extends Annotation>(arg0: Class<A>): A;

        getDeclaredAnnotationsByType<A extends Annotation>(arg0: Class<A>): A[];

        getDeclaredAnnotations(): Annotation[];
    }

    export abstract class Process {
        constructor();

        abstract getOutputStream(): OutputStream;

        abstract getInputStream(): InputStream;

        abstract getErrorStream(): InputStream;

        inputReader(): BufferedReader;

        inputReader(arg0: Charset): BufferedReader;

        errorReader(): BufferedReader;

        errorReader(arg0: Charset): BufferedReader;

        outputWriter(): BufferedWriter;

        outputWriter(arg0: Charset): BufferedWriter;

        abstract waitFor(): number;

        waitFor(arg0: number, arg1: TimeUnit): boolean;

        abstract exitValue(): number;

        abstract destroy(): void;

        destroyForcibly(): Process;

        supportsNormalTermination(): boolean;

        isAlive(): boolean;

        pid(): number;

        onExit(): CompletableFuture<Process>;

        toHandle(): ProcessHandle;

        info(): ProcessHandle.Info;

        children(): Stream<ProcessHandle>;

        descendants(): Stream<ProcessHandle>;
    }

    export class ProcessBuilder {
        constructor(arg0: List<String>);
        constructor(arg0: String[]);

        command(arg0: List<String>): ProcessBuilder;

        command(arg0: String[]): ProcessBuilder;

        command(): List<String>;

        environment(): Map<String, String>;

        directory(): File;

        directory(arg0: File): ProcessBuilder;

        redirectInput(arg0: ProcessBuilder.Redirect): ProcessBuilder;

        redirectOutput(arg0: ProcessBuilder.Redirect): ProcessBuilder;

        redirectError(arg0: ProcessBuilder.Redirect): ProcessBuilder;

        redirectInput(arg0: File): ProcessBuilder;

        redirectOutput(arg0: File): ProcessBuilder;

        redirectError(arg0: File): ProcessBuilder;

        redirectInput(): ProcessBuilder.Redirect;

        redirectOutput(): ProcessBuilder.Redirect;

        redirectError(): ProcessBuilder.Redirect;

        inheritIO(): ProcessBuilder;

        redirectErrorStream(): boolean;

        redirectErrorStream(arg0: boolean): ProcessBuilder;

        start(): Process;

        static startPipeline(arg0: List<ProcessBuilder>): List<Process>;
    }
    export namespace ProcessBuilder {
        export abstract class Redirect {
            static PIPE: ProcessBuilder.Redirect
            static INHERIT: ProcessBuilder.Redirect
            static DISCARD: ProcessBuilder.Redirect

            abstract type(): ProcessBuilder.Redirect.Type;

            file(): File;

            static from(arg0: File): ProcessBuilder.Redirect;

            static to(arg0: File): ProcessBuilder.Redirect;

            static appendTo(arg0: File): ProcessBuilder.Redirect;

            equals(arg0: Object): boolean;

            hashCode(): number;
        }
        export namespace Redirect {
            export class Type extends Enum<ProcessBuilder.Redirect.Type> {
                static PIPE: ProcessBuilder.Redirect.Type
                static INHERIT: ProcessBuilder.Redirect.Type
                static READ: ProcessBuilder.Redirect.Type
                static WRITE: ProcessBuilder.Redirect.Type
                static APPEND: ProcessBuilder.Redirect.Type

                static values(): ProcessBuilder.Redirect.Type[];

                static valueOf(arg0: String): ProcessBuilder.Redirect.Type;
                /**
                * DO NOT USE
                */
                static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
            }

        }

        export class Type extends Enum<ProcessBuilder.Redirect.Type> {
            static PIPE: ProcessBuilder.Redirect.Type
            static INHERIT: ProcessBuilder.Redirect.Type
            static READ: ProcessBuilder.Redirect.Type
            static WRITE: ProcessBuilder.Redirect.Type
            static APPEND: ProcessBuilder.Redirect.Type

            static values(): ProcessBuilder.Redirect.Type[];

            static valueOf(arg0: String): ProcessBuilder.Redirect.Type;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export namespace ProcessHandle {
        function
/* default */ of(arg0: number): Optional<ProcessHandle>;
        function
/* default */ current(): ProcessHandle;
        function
/* default */ allProcesses(): Stream<ProcessHandle>;
    }

    export interface ProcessHandle extends Comparable<ProcessHandle>, Object {

        pid(): number;

        parent(): Optional<ProcessHandle>;

        children(): Stream<ProcessHandle>;

        descendants(): Stream<ProcessHandle>;

        info(): ProcessHandle.Info;

        onExit(): CompletableFuture<ProcessHandle>;

        supportsNormalTermination(): boolean;

        destroy(): boolean;

        destroyForcibly(): boolean;

        isAlive(): boolean;

        hashCode(): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: ProcessHandle): number;
    }
    export namespace ProcessHandle {
        export interface Info {

            command(): Optional<String>;

            commandLine(): Optional<String>;

            arguments(): Optional<String[]>;

            startInstant(): Optional<Instant>;

            totalCpuDuration(): Optional<Duration>;

            user(): Optional<String>;
        }

    }

    export interface Readable {

        read(arg0: CharBuffer): number;
    }

    export abstract class Record {

        abstract equals(arg0: Object): boolean;

        abstract hashCode(): number;
        toString(): string;
    }

    export class ReflectiveOperationException extends Exception {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export interface Runnable {

        run(): void;
    }

    export class Runtime {

        static getRuntime(): Runtime;

        exit(arg0: number): void;

        addShutdownHook(arg0: Thread): void;

        removeShutdownHook(arg0: Thread): boolean;

        halt(arg0: number): void;

        exec(arg0: String): Process;

        exec(arg0: String, arg1: String[]): Process;

        exec(arg0: String, arg1: String[], arg2: File): Process;

        exec(arg0: String[]): Process;

        exec(arg0: String[], arg1: String[]): Process;

        exec(arg0: String[], arg1: String[], arg2: File): Process;

        availableProcessors(): number;

        freeMemory(): number;

        totalMemory(): number;

        maxMemory(): number;

        gc(): void;

        runFinalization(): void;

        load(arg0: String): void;

        loadLibrary(arg0: String): void;

        static version(): Runtime.Version;
    }
    export namespace Runtime {
        export class Version extends Object implements Comparable<Runtime.Version> {

            static parse(arg0: String): Runtime.Version;

            feature(): number;

            interim(): number;

            update(): number;

            patch(): number;

            major(): number;

            minor(): number;

            security(): number;

            version(): List<Number>;

            pre(): Optional<String>;

            build(): Optional<Number>;

            optional(): Optional<String>;

            compareTo(arg0: Runtime.Version): number;

            compareToIgnoreOptional(arg0: Runtime.Version): number;
            toString(): string;

            equals(arg0: Object): boolean;

            equalsIgnoreOptional(arg0: Object): boolean;

            hashCode(): number;
        }

    }

    export class RuntimeException extends Exception {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class RuntimePermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }


    export class SecurityException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class SecurityManager {
        constructor();

        getSecurityContext(): Object;

        checkPermission(arg0: Permission): void;

        checkPermission(arg0: Permission, arg1: Object): void;

        checkCreateClassLoader(): void;

        checkAccess(arg0: Thread): void;

        checkAccess(arg0: ThreadGroup): void;

        checkExit(arg0: number): void;

        checkExec(arg0: String): void;

        checkLink(arg0: String): void;

        checkRead(arg0: FileDescriptor): void;

        checkRead(arg0: String): void;

        checkRead(arg0: String, arg1: Object): void;

        checkWrite(arg0: FileDescriptor): void;

        checkWrite(arg0: String): void;

        checkDelete(arg0: String): void;

        checkConnect(arg0: String, arg1: number): void;

        checkConnect(arg0: String, arg1: number, arg2: Object): void;

        checkListen(arg0: number): void;

        checkAccept(arg0: String, arg1: number): void;

        checkMulticast(arg0: InetAddress): void;

        checkMulticast(arg0: InetAddress, arg1: number): void;

        checkPropertiesAccess(): void;

        checkPropertyAccess(arg0: String): void;

        checkPrintJobAccess(): void;

        checkPackageAccess(arg0: String): void;

        checkPackageDefinition(arg0: String): void;

        checkSetFactory(): void;

        checkSecurityAccess(arg0: String): void;

        getThreadGroup(): ThreadGroup;
    }

    export class Short extends Number implements Comparable<Number>, Constable {
        static MIN_VALUE: number
        static MAX_VALUE: number
        static TYPE: Class<Number>
        static SIZE: number
        static BYTES: number
        constructor(arg0: number);
        constructor(arg0: String);

        static toString(arg0: number): String;

        static parseShort(arg0: String, arg1: number): number;

        static parseShort(arg0: String): number;

        static valueOf(arg0: String, arg1: number): Number;

        static valueOf(arg0: String): Number;

        describeConstable(): Optional<DynamicConstantDesc<Number>>;

        static valueOf(arg0: number): Number;

        static decode(arg0: String): Number;

        byteValue(): number;

        shortValue(): number;

        intValue(): number;

        longValue(): number;

        floatValue(): number;

        doubleValue(): number;
        toString(): string;

        hashCode(): number;

        static hashCode(arg0: number): number;

        equals(arg0: Object): boolean;

        compareTo(arg0: Number): number;

        static compare(arg0: number, arg1: number): number;

        static compareUnsigned(arg0: number, arg1: number): number;

        static reverseBytes(arg0: number): number;

        static toUnsignedInt(arg0: number): number;

        static toUnsignedLong(arg0: number): number;
    }

    export class StackOverflowError extends VirtualMachineError {
        constructor();
        constructor(arg0: String);
    }

    export class StackTraceElement implements Serializable {
        constructor(arg0: String, arg1: String, arg2: String, arg3: number);
        constructor(arg0: String, arg1: String, arg2: String, arg3: String, arg4: String, arg5: String, arg6: number);

        getFileName(): String;

        getLineNumber(): number;

        getModuleName(): String;

        getModuleVersion(): String;

        getClassLoaderName(): String;

        getClassName(): String;

        getMethodName(): String;

        isNativeMethod(): boolean;
        toString(): string;

        equals(arg0: Object): boolean;

        hashCode(): number;
    }

    export class StackWalker {

        static getInstance(): StackWalker;

        static getInstance(arg0: StackWalker.Option): StackWalker;

        static getInstance(arg0: Set<StackWalker.Option>): StackWalker;

        static getInstance(arg0: Set<StackWalker.Option>, arg1: number): StackWalker;

        walk<T extends Object>(arg0: Function<Stream<StackWalker.StackFrame>, T>): T;

        forEach(arg0: Consumer<StackWalker.StackFrame>): void;

        getCallerClass(): Class<any>;
    }
    export namespace StackWalker {
        export class Option extends Enum<StackWalker.Option> {
            static RETAIN_CLASS_REFERENCE: StackWalker.Option
            static SHOW_REFLECT_FRAMES: StackWalker.Option
            static SHOW_HIDDEN_FRAMES: StackWalker.Option

            static values(): StackWalker.Option[];

            static valueOf(arg0: String): StackWalker.Option;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export interface StackFrame {

            getClassName(): String;

            getMethodName(): String;

            getDeclaringClass(): Class<any>;

/* default */ getMethodType(): MethodType;

/* default */ getDescriptor(): String;

            getByteCodeIndex(): number;

            getFileName(): String;

            getLineNumber(): number;

            isNativeMethod(): boolean;

            toStackTraceElement(): StackTraceElement;
        }

    }

    export class StrictMath {
        static E: number
        static PI: number

        static sin(arg0: number): number;

        static cos(arg0: number): number;

        static tan(arg0: number): number;

        static asin(arg0: number): number;

        static acos(arg0: number): number;

        static atan(arg0: number): number;

        static toRadians(arg0: number): number;

        static toDegrees(arg0: number): number;

        static exp(arg0: number): number;

        static log(arg0: number): number;

        static log10(arg0: number): number;

        static sqrt(arg0: number): number;

        static cbrt(arg0: number): number;

        static IEEEremainder(arg0: number, arg1: number): number;

        static ceil(arg0: number): number;

        static floor(arg0: number): number;

        static rint(arg0: number): number;

        static atan2(arg0: number, arg1: number): number;

        static pow(arg0: number, arg1: number): number;

        static round(arg0: number): number;

        static round(arg0: number): number;

        static random(): number;

        static addExact(arg0: number, arg1: number): number;

        static addExact(arg0: number, arg1: number): number;

        static subtractExact(arg0: number, arg1: number): number;

        static subtractExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static multiplyExact(arg0: number, arg1: number): number;

        static incrementExact(arg0: number): number;

        static incrementExact(arg0: number): number;

        static decrementExact(arg0: number): number;

        static decrementExact(arg0: number): number;

        static negateExact(arg0: number): number;

        static negateExact(arg0: number): number;

        static toIntExact(arg0: number): number;

        static multiplyFull(arg0: number, arg1: number): number;

        static multiplyHigh(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorDiv(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static floorMod(arg0: number, arg1: number): number;

        static abs(arg0: number): number;

        static absExact(arg0: number): number;

        static abs(arg0: number): number;

        static absExact(arg0: number): number;

        static abs(arg0: number): number;

        static abs(arg0: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static max(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static min(arg0: number, arg1: number): number;

        static fma(arg0: number, arg1: number, arg2: number): number;

        static fma(arg0: number, arg1: number, arg2: number): number;

        static ulp(arg0: number): number;

        static ulp(arg0: number): number;

        static signum(arg0: number): number;

        static signum(arg0: number): number;

        static sinh(arg0: number): number;

        static cosh(arg0: number): number;

        static tanh(arg0: number): number;

        static hypot(arg0: number, arg1: number): number;

        static expm1(arg0: number): number;

        static log1p(arg0: number): number;

        static copySign(arg0: number, arg1: number): number;

        static copySign(arg0: number, arg1: number): number;

        static getExponent(arg0: number): number;

        static getExponent(arg0: number): number;

        static nextAfter(arg0: number, arg1: number): number;

        static nextAfter(arg0: number, arg1: number): number;

        static nextUp(arg0: number): number;

        static nextUp(arg0: number): number;

        static nextDown(arg0: number): number;

        static nextDown(arg0: number): number;

        static scalb(arg0: number, arg1: number): number;

        static scalb(arg0: number, arg1: number): number;
    }

    export class String extends Object implements Serializable, Comparable<String>, CharSequence, Constable, ConstantDesc {
        static CASE_INSENSITIVE_ORDER: Comparator<String>
        constructor();
        constructor(arg0: String);
        constructor(arg0: String[]);
        constructor(arg0: String[], arg1: number, arg2: number);
        constructor(arg0: number[], arg1: number, arg2: number);
        constructor(arg0: number[], arg1: number, arg2: number, arg3: number);
        constructor(arg0: number[], arg1: number);
        constructor(arg0: number[], arg1: number, arg2: number, arg3: String);
        constructor(arg0: number[], arg1: number, arg2: number, arg3: Charset);
        constructor(arg0: number[], arg1: String);
        constructor(arg0: number[], arg1: Charset);
        constructor(arg0: number[], arg1: number, arg2: number);
        constructor(arg0: number[]);
        constructor(arg0: StringBuffer);
        constructor(arg0: StringBuilder);

        length(): number;

        isEmpty(): boolean;

        charAt(arg0: number): String;

        codePointAt(arg0: number): number;

        codePointBefore(arg0: number): number;

        codePointCount(arg0: number, arg1: number): number;

        offsetByCodePoints(arg0: number, arg1: number): number;

        getChars(arg0: number, arg1: number, arg2: String[], arg3: number): void;

        getBytes(arg0: number, arg1: number, arg2: number[], arg3: number): void;

        getBytes(arg0: String): number[];

        getBytes(arg0: Charset): number[];

        getBytes(): number[];

        equals(arg0: Object): boolean;

        contentEquals(arg0: StringBuffer): boolean;

        contentEquals(arg0: CharSequence): boolean;

        equalsIgnoreCase(arg0: String): boolean;

        compareTo(arg0: String): number;

        compareToIgnoreCase(arg0: String): number;

        regionMatches(arg0: number, arg1: String, arg2: number, arg3: number): boolean;

        regionMatches(arg0: boolean, arg1: number, arg2: String, arg3: number, arg4: number): boolean;

        startsWith(arg0: String, arg1: number): boolean;

        startsWith(arg0: String): boolean;

        endsWith(arg0: String): boolean;

        hashCode(): number;

        indexOf(arg0: number): number;

        indexOf(arg0: number, arg1: number): number;

        lastIndexOf(arg0: number): number;

        lastIndexOf(arg0: number, arg1: number): number;

        indexOf(arg0: String): number;

        indexOf(arg0: String, arg1: number): number;

        lastIndexOf(arg0: String): number;

        lastIndexOf(arg0: String, arg1: number): number;

        substring(arg0: number): String;

        substring(arg0: number, arg1: number): String;

        subSequence(arg0: number, arg1: number): CharSequence;

        concat(arg0: String): String;

        replace(arg0: String, arg1: String): String;

        matches(arg0: String): boolean;

        contains(arg0: CharSequence): boolean;

        replaceFirst(arg0: String, arg1: String): String;

        replaceAll(arg0: String, arg1: String): String;

        replace(arg0: CharSequence, arg1: CharSequence): String;

        split(arg0: String, arg1: number): String[];

        split(arg0: String): String[];

        static join(arg0: CharSequence, arg1: CharSequence[]): String;

        static join(arg0: CharSequence, arg1: Iterable<CharSequence>): String;

        toLowerCase(arg0: Locale): String;

        toLowerCase(): String;

        toUpperCase(arg0: Locale): String;

        toUpperCase(): String;

        trim(): String;

        strip(): String;

        stripLeading(): String;

        stripTrailing(): String;

        isBlank(): boolean;

        lines(): Stream<String>;

        indent(arg0: number): String;

        stripIndent(): String;

        translateEscapes(): String;

        transform<R extends Object>(arg0: Function<String, R>): R;
        toString(): string;

        chars(): IntStream;

        codePoints(): IntStream;

        toCharArray(): String[];

        static format(arg0: String, arg1: Object[]): String;

        static format(arg0: Locale, arg1: String, arg2: Object[]): String;

        formatted(arg0: Object[]): String;

        static valueOf(arg0: Object): String;

        static valueOf(arg0: String[]): String;

        static valueOf(arg0: String[], arg1: number, arg2: number): String;

        static copyValueOf(arg0: String[], arg1: number, arg2: number): String;

        static copyValueOf(arg0: String[]): String;

        static valueOf(arg0: boolean): String;

        static valueOf(arg0: String): String;

        static valueOf(arg0: number): String;

        static valueOf(arg0: number): String;

        static valueOf(arg0: number): String;

        static valueOf(arg0: number): String;

        intern(): String;

        repeat(arg0: number): String;

        describeConstable(): Optional<String>;

        resolveConstantDesc(arg0: Lookup): String;
    }

    export interface StringBuffer extends Serializable, Comparable<StringBuffer>, CharSequence { }
    export class StringBuffer extends AbstractStringBuilder implements Serializable, Comparable<StringBuffer>, CharSequence {
        constructor();
        constructor(arg0: number);
        constructor(arg0: String);
        constructor(arg0: CharSequence);

        compareTo(arg0: StringBuffer): number;

        length(): number;

        capacity(): number;

        ensureCapacity(arg0: number): void;

        trimToSize(): void;

        setLength(arg0: number): void;

        charAt(arg0: number): String;

        codePointAt(arg0: number): number;

        codePointBefore(arg0: number): number;

        codePointCount(arg0: number, arg1: number): number;

        offsetByCodePoints(arg0: number, arg1: number): number;

        getChars(arg0: number, arg1: number, arg2: String[], arg3: number): void;

        setCharAt(arg0: number, arg1: String): void;

        append(arg0: Object): StringBuffer;

        append(arg0: String): StringBuffer;

        append(arg0: StringBuffer): StringBuffer;

        append(arg0: CharSequence): StringBuffer;

        append(arg0: CharSequence, arg1: number, arg2: number): StringBuffer;

        append(arg0: String[]): StringBuffer;

        append(arg0: String[], arg1: number, arg2: number): StringBuffer;

        append(arg0: boolean): StringBuffer;

        append(arg0: String): StringBuffer;

        append(arg0: number): StringBuffer;

        appendCodePoint(arg0: number): StringBuffer;

        append(arg0: number): StringBuffer;

        append(arg0: number): StringBuffer;

        append(arg0: number): StringBuffer;

        delete(arg0: number, arg1: number): StringBuffer;

        deleteCharAt(arg0: number): StringBuffer;

        replace(arg0: number, arg1: number, arg2: String): StringBuffer;

        substring(arg0: number): String;

        subSequence(arg0: number, arg1: number): CharSequence;

        substring(arg0: number, arg1: number): String;

        insert(arg0: number, arg1: String[], arg2: number, arg3: number): StringBuffer;

        insert(arg0: number, arg1: Object): StringBuffer;

        insert(arg0: number, arg1: String): StringBuffer;

        insert(arg0: number, arg1: String[]): StringBuffer;

        insert(arg0: number, arg1: CharSequence): StringBuffer;

        insert(arg0: number, arg1: CharSequence, arg2: number, arg3: number): StringBuffer;

        insert(arg0: number, arg1: boolean): StringBuffer;

        insert(arg0: number, arg1: String): StringBuffer;

        insert(arg0: number, arg1: number): StringBuffer;

        insert(arg0: number, arg1: number): StringBuffer;

        insert(arg0: number, arg1: number): StringBuffer;

        insert(arg0: number, arg1: number): StringBuffer;

        indexOf(arg0: String): number;

        indexOf(arg0: String, arg1: number): number;

        lastIndexOf(arg0: String): number;

        lastIndexOf(arg0: String, arg1: number): number;

        reverse(): StringBuffer;
        toString(): string;
    }

    export interface StringBuilder extends Serializable, Comparable<StringBuilder>, CharSequence { }
    export class StringBuilder extends AbstractStringBuilder implements Serializable, Comparable<StringBuilder>, CharSequence {
        constructor();
        constructor(arg0: number);
        constructor(arg0: String);
        constructor(arg0: CharSequence);

        compareTo(arg0: StringBuilder): number;

        append(arg0: Object): StringBuilder;

        append(arg0: String): StringBuilder;

        append(arg0: StringBuffer): StringBuilder;

        append(arg0: CharSequence): StringBuilder;

        append(arg0: CharSequence, arg1: number, arg2: number): StringBuilder;

        append(arg0: String[]): StringBuilder;

        append(arg0: String[], arg1: number, arg2: number): StringBuilder;

        append(arg0: boolean): StringBuilder;

        append(arg0: String): StringBuilder;

        append(arg0: number): StringBuilder;

        append(arg0: number): StringBuilder;

        append(arg0: number): StringBuilder;

        append(arg0: number): StringBuilder;

        appendCodePoint(arg0: number): StringBuilder;

        delete(arg0: number, arg1: number): StringBuilder;

        deleteCharAt(arg0: number): StringBuilder;

        replace(arg0: number, arg1: number, arg2: String): StringBuilder;

        insert(arg0: number, arg1: String[], arg2: number, arg3: number): StringBuilder;

        insert(arg0: number, arg1: Object): StringBuilder;

        insert(arg0: number, arg1: String): StringBuilder;

        insert(arg0: number, arg1: String[]): StringBuilder;

        insert(arg0: number, arg1: CharSequence): StringBuilder;

        insert(arg0: number, arg1: CharSequence, arg2: number, arg3: number): StringBuilder;

        insert(arg0: number, arg1: boolean): StringBuilder;

        insert(arg0: number, arg1: String): StringBuilder;

        insert(arg0: number, arg1: number): StringBuilder;

        insert(arg0: number, arg1: number): StringBuilder;

        insert(arg0: number, arg1: number): StringBuilder;

        insert(arg0: number, arg1: number): StringBuilder;

        indexOf(arg0: String): number;

        indexOf(arg0: String, arg1: number): number;

        lastIndexOf(arg0: String): number;

        lastIndexOf(arg0: String, arg1: number): number;

        reverse(): StringBuilder;
        toString(): string;
    }

    export class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: number);
    }


    export class System {
        static in: InputStream
        static out: PrintStream
        static err: PrintStream

        static setIn(arg0: InputStream): void;

        static setOut(arg0: PrintStream): void;

        static setErr(arg0: PrintStream): void;

        static console(): Console;

        static inheritedChannel(): Channel;

        static setSecurityManager(arg0: SecurityManager): void;

        static getSecurityManager(): SecurityManager;

        static currentTimeMillis(): number;

        static nanoTime(): number;

        static arraycopy(arg0: Object, arg1: number, arg2: Object, arg3: number, arg4: number): void;

        static identityHashCode(arg0: Object): number;

        static getProperties(): Properties;

        static lineSeparator(): String;

        static setProperties(arg0: Properties): void;

        static getProperty(arg0: String): String;

        static getProperty(arg0: String, arg1: String): String;

        static setProperty(arg0: String, arg1: String): String;

        static clearProperty(arg0: String): String;

        static getenv(arg0: String): String;

        static getenv(): Map<String, String>;

        static getLogger(arg0: String): System.Logger;

        static getLogger(arg0: String, arg1: ResourceBundle): System.Logger;

        static exit(arg0: number): void;

        static gc(): void;

        static runFinalization(): void;

        static load(arg0: String): void;

        static loadLibrary(arg0: String): void;

        static mapLibraryName(arg0: String): String;
    }
    export namespace System {
        export interface Logger {

            getName(): String;

            isLoggable(arg0: System.Logger.Level): boolean;

/* default */ log(arg0: System.Logger.Level, arg1: String): void;

/* default */ log(arg0: System.Logger.Level, arg1: Supplier<String>): void;

/* default */ log(arg0: System.Logger.Level, arg1: Object): void;

/* default */ log(arg0: System.Logger.Level, arg1: String, arg2: Throwable): void;

/* default */ log(arg0: System.Logger.Level, arg1: Supplier<String>, arg2: Throwable): void;

/* default */ log(arg0: System.Logger.Level, arg1: String, arg2: Object[]): void;

            log(arg0: System.Logger.Level, arg1: ResourceBundle, arg2: String, arg3: Throwable): void;

            log(arg0: System.Logger.Level, arg1: ResourceBundle, arg2: String, arg3: Object[]): void;
        }
        export namespace Logger {
            export class Level extends Enum<System.Logger.Level> {
                static ALL: System.Logger.Level
                static TRACE: System.Logger.Level
                static DEBUG: System.Logger.Level
                static INFO: System.Logger.Level
                static WARNING: System.Logger.Level
                static ERROR: System.Logger.Level
                static OFF: System.Logger.Level

                static values(): System.Logger.Level[];

                static valueOf(arg0: String): System.Logger.Level;

                getName(): String;

                getSeverity(): number;
                /**
                * DO NOT USE
                */
                static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
            }

        }

        export class Level extends Enum<System.Logger.Level> {
            static ALL: System.Logger.Level
            static TRACE: System.Logger.Level
            static DEBUG: System.Logger.Level
            static INFO: System.Logger.Level
            static WARNING: System.Logger.Level
            static ERROR: System.Logger.Level
            static OFF: System.Logger.Level

            static values(): System.Logger.Level[];

            static valueOf(arg0: String): System.Logger.Level;

            getName(): String;

            getSeverity(): number;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export abstract class LoggerFinder {

            abstract getLogger(arg0: String, arg1: Module): System.Logger;

            getLocalizedLogger(arg0: String, arg1: ResourceBundle, arg2: Module): System.Logger;

            static getLoggerFinder(): System.LoggerFinder;
        }

    }

    export class Thread implements Runnable {
        static MIN_PRIORITY: number
        static NORM_PRIORITY: number
        static MAX_PRIORITY: number
        constructor();
        constructor(arg0: Runnable);
        constructor(arg0: ThreadGroup, arg1: Runnable);
        constructor(arg0: String);
        constructor(arg0: ThreadGroup, arg1: String);
        constructor(arg0: Runnable, arg1: String);
        constructor(arg0: ThreadGroup, arg1: Runnable, arg2: String);
        constructor(arg0: ThreadGroup, arg1: Runnable, arg2: String, arg3: number);
        constructor(arg0: ThreadGroup, arg1: Runnable, arg2: String, arg3: number, arg4: boolean);

        static currentThread(): Thread;

        static yield(): void;

        static sleep(arg0: number): void;

        static sleep(arg0: number, arg1: number): void;

        static onSpinWait(): void;

        start(): void;

        run(): void;

        stop(): void;

        interrupt(): void;

        static interrupted(): boolean;

        isInterrupted(): boolean;

        isAlive(): boolean;

        suspend(): void;

        resume(): void;

        setPriority(arg0: number): void;

        getPriority(): number;

        setName(arg0: String): void;

        getName(): String;

        getThreadGroup(): ThreadGroup;

        static activeCount(): number;

        static enumerate(arg0: Thread[]): number;

        countStackFrames(): number;

        join(arg0: number): void;

        join(arg0: number, arg1: number): void;

        join(): void;

        static dumpStack(): void;

        setDaemon(arg0: boolean): void;

        isDaemon(): boolean;

        checkAccess(): void;
        toString(): string;

        getContextClassLoader(): ClassLoader;

        setContextClassLoader(arg0: ClassLoader): void;

        static holdsLock(arg0: Object): boolean;

        getStackTrace(): StackTraceElement[];

        static getAllStackTraces(): Map<Thread, StackTraceElement[]>;

        getId(): number;

        getState(): Thread.State;

        static setDefaultUncaughtExceptionHandler(arg0: Thread.UncaughtExceptionHandler): void;

        static getDefaultUncaughtExceptionHandler(): Thread.UncaughtExceptionHandler;

        getUncaughtExceptionHandler(): Thread.UncaughtExceptionHandler;

        setUncaughtExceptionHandler(arg0: Thread.UncaughtExceptionHandler): void;
    }
    export namespace Thread {
        export class State extends Enum<Thread.State> {
            static NEW: Thread.State
            static RUNNABLE: Thread.State
            static BLOCKED: Thread.State
            static WAITING: Thread.State
            static TIMED_WAITING: Thread.State
            static TERMINATED: Thread.State

            static values(): Thread.State[];

            static valueOf(arg0: String): Thread.State;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

        export interface UncaughtExceptionHandler {

            uncaughtException(arg0: Thread, arg1: Throwable): void;
        }

    }

    export class ThreadDeath extends Error {
        constructor();
    }

    export class ThreadGroup implements Thread.UncaughtExceptionHandler {
        constructor(arg0: String);
        constructor(arg0: ThreadGroup, arg1: String);

        getName(): String;

        getParent(): ThreadGroup;

        getMaxPriority(): number;

        isDaemon(): boolean;

        isDestroyed(): boolean;

        setDaemon(arg0: boolean): void;

        setMaxPriority(arg0: number): void;

        parentOf(arg0: ThreadGroup): boolean;

        checkAccess(): void;

        activeCount(): number;

        enumerate(arg0: Thread[]): number;

        enumerate(arg0: Thread[], arg1: boolean): number;

        activeGroupCount(): number;

        enumerate(arg0: ThreadGroup[]): number;

        enumerate(arg0: ThreadGroup[], arg1: boolean): number;

        stop(): void;

        interrupt(): void;

        suspend(): void;

        resume(): void;

        destroy(): void;

        list(): void;

        uncaughtException(arg0: Thread, arg1: Throwable): void;

        allowThreadSuspension(arg0: boolean): boolean;
        toString(): string;
    }

    export class ThreadLocal<T extends Object> extends Object {
        constructor();

        static withInitial<S extends Object>(arg0: Supplier<S>): ThreadLocal<S>;

        get(): T;

        set(arg0: T): void;

        remove(): void;
    }

    export class Throwable implements Serializable {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);

        getMessage(): String;

        getLocalizedMessage(): String;

        getCause(): Throwable;

        initCause(arg0: Throwable): Throwable;
        toString(): string;

        printStackTrace(): void;

        printStackTrace(arg0: PrintStream): void;

        printStackTrace(arg0: PrintWriter): void;

        fillInStackTrace(): Throwable;

        getStackTrace(): StackTraceElement[];

        setStackTrace(arg0: StackTraceElement[]): void;

        addSuppressed(arg0: Throwable): void;

        getSuppressed(): Throwable[];
    }

    export class TypeNotPresentException extends RuntimeException {
        constructor(arg0: String, arg1: Throwable);

        typeName(): String;
    }

    export class UnknownError extends VirtualMachineError {
        constructor();
        constructor(arg0: String);
    }

    export class UnsatisfiedLinkError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export class UnsupportedClassVersionError extends ClassFormatError {
        constructor();
        constructor(arg0: String);
    }

    export class UnsupportedOperationException extends RuntimeException {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class VerifyError extends LinkageError {
        constructor();
        constructor(arg0: String);
    }

    export abstract class VirtualMachineError extends Error {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class Void {
        static TYPE: Class<Void>
    }

}
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.lang.invoke.d.ts" />
declare module '@java/java.lang.constant' {
    import { Enum, Class, String, Boolean } from '@java/java.lang'
    import { Optional, List } from '@java/java.util'
    import { TypeDescriptor, Lookup, CallSite } from '@java/java.lang.invoke'
    export namespace ClassDesc {
        function
/* default */ of(arg0: String): ClassDesc;
        function
/* default */ of(arg0: String, arg1: String): ClassDesc;
        function
/* default */ ofDescriptor(arg0: String): ClassDesc;
    }

    export interface ClassDesc extends ConstantDesc, TypeDescriptor.OfField<ClassDesc>, Object {

/* default */ arrayType(): ClassDesc;

/* default */ arrayType(arg0: number): ClassDesc;

/* default */ nested(arg0: String): ClassDesc;

/* default */ nested(arg0: String, arg1: String[]): ClassDesc;

/* default */ isArray(): boolean;

/* default */ isPrimitive(): boolean;

/* default */ isClassOrInterface(): boolean;

/* default */ componentType(): ClassDesc;

/* default */ packageName(): String;

/* default */ displayName(): String;

        descriptorString(): String;

        equals(arg0: Object): boolean;
    }

    export interface Constable {

        describeConstable(): Optional<ConstantDesc>;
    }

    export interface ConstantDesc {

        resolveConstantDesc(arg0: Lookup): Object;
    }

    export class ConstantDescs {
        static DEFAULT_NAME: String
        static CD_Object: ClassDesc
        static CD_String: ClassDesc
        static CD_Class: ClassDesc
        static CD_Number: ClassDesc
        static CD_Integer: ClassDesc
        static CD_Long: ClassDesc
        static CD_Float: ClassDesc
        static CD_Double: ClassDesc
        static CD_Short: ClassDesc
        static CD_Byte: ClassDesc
        static CD_Character: ClassDesc
        static CD_Boolean: ClassDesc
        static CD_Void: ClassDesc
        static CD_Throwable: ClassDesc
        static CD_Exception: ClassDesc
        static CD_Enum: ClassDesc
        static CD_VarHandle: ClassDesc
        static CD_MethodHandles: ClassDesc
        static CD_MethodHandles_Lookup: ClassDesc
        static CD_MethodHandle: ClassDesc
        static CD_MethodType: ClassDesc
        static CD_CallSite: ClassDesc
        static CD_Collection: ClassDesc
        static CD_List: ClassDesc
        static CD_Set: ClassDesc
        static CD_Map: ClassDesc
        static CD_ConstantDesc: ClassDesc
        static CD_ClassDesc: ClassDesc
        static CD_EnumDesc: ClassDesc
        static CD_MethodTypeDesc: ClassDesc
        static CD_MethodHandleDesc: ClassDesc
        static CD_DirectMethodHandleDesc: ClassDesc
        static CD_VarHandleDesc: ClassDesc
        static CD_MethodHandleDesc_Kind: ClassDesc
        static CD_DynamicConstantDesc: ClassDesc
        static CD_DynamicCallSiteDesc: ClassDesc
        static CD_ConstantBootstraps: ClassDesc
        static BSM_PRIMITIVE_CLASS: DirectMethodHandleDesc
        static BSM_ENUM_CONSTANT: DirectMethodHandleDesc
        static BSM_GET_STATIC_FINAL: DirectMethodHandleDesc
        static BSM_NULL_CONSTANT: DirectMethodHandleDesc
        static BSM_VARHANDLE_FIELD: DirectMethodHandleDesc
        static BSM_VARHANDLE_STATIC_FIELD: DirectMethodHandleDesc
        static BSM_VARHANDLE_ARRAY: DirectMethodHandleDesc
        static BSM_INVOKE: DirectMethodHandleDesc
        static BSM_EXPLICIT_CAST: DirectMethodHandleDesc
        static CD_int: ClassDesc
        static CD_long: ClassDesc
        static CD_float: ClassDesc
        static CD_double: ClassDesc
        static CD_short: ClassDesc
        static CD_byte: ClassDesc
        static CD_char: ClassDesc
        static CD_boolean: ClassDesc
        static CD_void: ClassDesc
        static NULL: ConstantDesc
        static TRUE: DynamicConstantDesc<Boolean>
        static FALSE: DynamicConstantDesc<Boolean>

        static ofCallsiteBootstrap(arg0: ClassDesc, arg1: String, arg2: ClassDesc, arg3: ClassDesc[]): DirectMethodHandleDesc;

        static ofConstantBootstrap(arg0: ClassDesc, arg1: String, arg2: ClassDesc, arg3: ClassDesc[]): DirectMethodHandleDesc;
    }

    export interface DirectMethodHandleDesc extends MethodHandleDesc {

        kind(): DirectMethodHandleDesc.Kind;

        refKind(): number;

        isOwnerInterface(): boolean;

        owner(): ClassDesc;

        methodName(): String;

        lookupDescriptor(): String;
    }
    export namespace DirectMethodHandleDesc {
        export class Kind extends Enum<DirectMethodHandleDesc.Kind> {
            static STATIC: DirectMethodHandleDesc.Kind
            static INTERFACE_STATIC: DirectMethodHandleDesc.Kind
            static VIRTUAL: DirectMethodHandleDesc.Kind
            static INTERFACE_VIRTUAL: DirectMethodHandleDesc.Kind
            static SPECIAL: DirectMethodHandleDesc.Kind
            static INTERFACE_SPECIAL: DirectMethodHandleDesc.Kind
            static CONSTRUCTOR: DirectMethodHandleDesc.Kind
            static GETTER: DirectMethodHandleDesc.Kind
            static SETTER: DirectMethodHandleDesc.Kind
            static STATIC_GETTER: DirectMethodHandleDesc.Kind
            static STATIC_SETTER: DirectMethodHandleDesc.Kind
            refKind: number
            isInterface: boolean

            static values(): DirectMethodHandleDesc.Kind[];

            static valueOf(arg0: String): DirectMethodHandleDesc.Kind;

            static valueOf(arg0: number): DirectMethodHandleDesc.Kind;

            static valueOf(arg0: number, arg1: boolean): DirectMethodHandleDesc.Kind;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class DynamicCallSiteDesc {

        static of(arg0: DirectMethodHandleDesc, arg1: String, arg2: MethodTypeDesc, arg3: ConstantDesc[]): DynamicCallSiteDesc;

        static of(arg0: DirectMethodHandleDesc, arg1: String, arg2: MethodTypeDesc): DynamicCallSiteDesc;

        static of(arg0: DirectMethodHandleDesc, arg1: MethodTypeDesc): DynamicCallSiteDesc;

        withArgs(arg0: ConstantDesc[]): DynamicCallSiteDesc;

        withNameAndType(arg0: String, arg1: MethodTypeDesc): DynamicCallSiteDesc;

        invocationName(): String;

        invocationType(): MethodTypeDesc;

        bootstrapMethod(): MethodHandleDesc;

        bootstrapArgs(): ConstantDesc[];

        resolveCallSiteDesc(arg0: Lookup): CallSite;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export abstract class DynamicConstantDesc<T extends Object> extends Object implements ConstantDesc {

        static ofCanonical<T extends Object>(arg0: DirectMethodHandleDesc, arg1: String, arg2: ClassDesc, arg3: ConstantDesc[]): ConstantDesc;

        static ofNamed<T extends Object>(arg0: DirectMethodHandleDesc, arg1: String, arg2: ClassDesc, arg3: ConstantDesc[]): DynamicConstantDesc<T>;

        static of<T extends Object>(arg0: DirectMethodHandleDesc, arg1: ConstantDesc[]): DynamicConstantDesc<T>;

        static of<T extends Object>(arg0: DirectMethodHandleDesc): DynamicConstantDesc<T>;

        constantName(): String;

        constantType(): ClassDesc;

        bootstrapMethod(): DirectMethodHandleDesc;

        bootstrapArgs(): ConstantDesc[];

        bootstrapArgsList(): List<ConstantDesc>;

        resolveConstantDesc(arg0: Lookup): T;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;
    }

    export namespace MethodHandleDesc {
        function
/* default */ of(arg0: DirectMethodHandleDesc.Kind, arg1: ClassDesc, arg2: String, arg3: String): DirectMethodHandleDesc;
        function
/* default */ ofMethod(arg0: DirectMethodHandleDesc.Kind, arg1: ClassDesc, arg2: String, arg3: MethodTypeDesc): DirectMethodHandleDesc;
        function
/* default */ ofField(arg0: DirectMethodHandleDesc.Kind, arg1: ClassDesc, arg2: String, arg3: ClassDesc): DirectMethodHandleDesc;
        function
/* default */ ofConstructor(arg0: ClassDesc, arg1: ClassDesc[]): DirectMethodHandleDesc;
    }

    export interface MethodHandleDesc extends ConstantDesc {

/* default */ asType(arg0: MethodTypeDesc): MethodHandleDesc;

        invocationType(): MethodTypeDesc;

        equals(arg0: Object): boolean;
    }

    export namespace MethodTypeDesc {
        function
/* default */ ofDescriptor(arg0: String): MethodTypeDesc;
        function
/* default */ of(arg0: ClassDesc, arg1: ClassDesc[]): MethodTypeDesc;
    }

    export interface MethodTypeDesc extends ConstantDesc, TypeDescriptor.OfMethod<ClassDesc, MethodTypeDesc>, Object {

        returnType(): ClassDesc;

        parameterCount(): number;

        parameterType(arg0: number): ClassDesc;

        parameterList(): List<ClassDesc>;

        parameterArray(): ClassDesc[];

        changeReturnType(arg0: ClassDesc): MethodTypeDesc;

        changeParameterType(arg0: number, arg1: ClassDesc): MethodTypeDesc;

        dropParameterTypes(arg0: number, arg1: number): MethodTypeDesc;

        insertParameterTypes(arg0: number, arg1: ClassDesc[]): MethodTypeDesc;

/* default */ descriptorString(): String;

/* default */ displayDescriptor(): String;

        equals(arg0: Object): boolean;
    }

}
/// <reference path="java.lang.reflect.d.ts" />
/// <reference path="java.lang.d.ts" />
declare module '@java/java.lang.annotation' {
    import { Method } from '@java/java.lang.reflect'
    import { Enum, Error, Throwable, Class, String, RuntimeException } from '@java/java.lang'
    export interface Annotation {

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        annotationType(): Class<Annotation>;
    }

    export class AnnotationFormatError extends Error {
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export class AnnotationTypeMismatchException extends RuntimeException {
        constructor(arg0: Method, arg1: String);

        element(): Method;

        foundType(): String;
    }


    export class ElementType extends Enum<ElementType> {
        static TYPE: ElementType
        static FIELD: ElementType
        static METHOD: ElementType
        static PARAMETER: ElementType
        static CONSTRUCTOR: ElementType
        static LOCAL_VARIABLE: ElementType
        static ANNOTATION_TYPE: ElementType
        static PACKAGE: ElementType
        static TYPE_PARAMETER: ElementType
        static TYPE_USE: ElementType
        static MODULE: ElementType
        static RECORD_COMPONENT: ElementType

        static values(): ElementType[];

        static valueOf(arg0: String): ElementType;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }

    export class IncompleteAnnotationException extends RuntimeException {
        constructor(arg0: Class<Annotation>, arg1: String);

        annotationType(): Class<Annotation>;

        elementName(): String;
    }





    export class RetentionPolicy extends Enum<RetentionPolicy> {
        static SOURCE: RetentionPolicy
        static CLASS: RetentionPolicy
        static RUNTIME: RetentionPolicy

        static values(): RetentionPolicy[];

        static valueOf(arg0: String): RetentionPolicy;
        /**
        * DO NOT USE
        */
        static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
    }


}
/// <reference path="java.security.d.ts" />
/// <reference path="java.lang.d.ts" />
/// <reference path="java.util.d.ts" />
/// <reference path="java.net.d.ts" />
/// <reference path="java.util.stream.d.ts" />
/// <reference path="java.nio.d.ts" />
/// <reference path="java.nio.channels.d.ts" />
/// <reference path="java.util.function.d.ts" />
/// <reference path="java.nio.file.d.ts" />
/// <reference path="java.nio.charset.d.ts" />
declare module '@java/java.io' {
    import { Permission, PermissionCollection, BasicPermission } from '@java/java.security'
    import { Enum, Comparable, AutoCloseable, Appendable, CharSequence, Error, String, Exception, StringBuffer, RuntimeException, Throwable, Class, Readable } from '@java/java.lang'
    import { Locale, Enumeration } from '@java/java.util'
    import { URI, URL } from '@java/java.net'
    import { Stream } from '@java/java.util.stream'
    import { CharBuffer } from '@java/java.nio'
    import { FileChannel } from '@java/java.nio.channels'
    import { BinaryOperator, Predicate } from '@java/java.util.function'
    import { Path } from '@java/java.nio.file'
    import { Charset, CharsetEncoder, CharsetDecoder } from '@java/java.nio.charset'
    export class BufferedInputStream extends FilterInputStream {
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: number);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        available(): number;

        mark(arg0: number): void;

        reset(): void;

        markSupported(): boolean;

        close(): void;
    }

    export class BufferedOutputStream extends FilterOutputStream {
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: number);

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;
    }

    export class BufferedReader extends Reader {
        constructor(arg0: Reader, arg1: number);
        constructor(arg0: Reader);

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        readLine(): String;

        skip(arg0: number): number;

        ready(): boolean;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;

        lines(): Stream<String>;
    }

    export class BufferedWriter extends Writer {
        constructor(arg0: Writer);
        constructor(arg0: Writer, arg1: number);

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String, arg1: number, arg2: number): void;

        newLine(): void;

        flush(): void;

        close(): void;
    }

    export class ByteArrayInputStream extends InputStream {
        constructor(arg0: number[]);
        constructor(arg0: number[], arg1: number, arg2: number);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        readAllBytes(): number[];

        readNBytes(arg0: number[], arg1: number, arg2: number): number;

        transferTo(arg0: OutputStream): number;

        skip(arg0: number): number;

        available(): number;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;
    }

    export class ByteArrayOutputStream extends OutputStream {
        constructor();
        constructor(arg0: number);

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        writeBytes(arg0: number[]): void;

        writeTo(arg0: OutputStream): void;

        reset(): void;

        toByteArray(): number[];

        size(): number;
        toString(): string;

        toString(arg0: String): String;

        toString(arg0: Charset): String;

        toString(arg0: number): String;

        close(): void;
    }

    export class CharArrayReader extends Reader {
        constructor(arg0: String[]);
        constructor(arg0: String[], arg1: number, arg2: number);

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        read(arg0: CharBuffer): number;

        skip(arg0: number): number;

        ready(): boolean;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;
    }

    export class CharArrayWriter extends Writer {
        constructor();
        constructor(arg0: number);

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String, arg1: number, arg2: number): void;

        writeTo(arg0: Writer): void;

        append(arg0: CharSequence): CharArrayWriter;

        append(arg0: CharSequence, arg1: number, arg2: number): CharArrayWriter;

        append(arg0: String): CharArrayWriter;

        reset(): void;

        toCharArray(): String[];

        size(): number;
        toString(): string;

        flush(): void;

        close(): void;
    }

    export class CharConversionException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export interface Closeable extends AutoCloseable {

        close(): void;
    }

    export class Console implements Flushable {

        writer(): PrintWriter;

        reader(): Reader;

        format(arg0: String, arg1: Object[]): Console;

        printf(arg0: String, arg1: Object[]): Console;

        readLine(arg0: String, arg1: Object[]): String;

        readLine(): String;

        readPassword(arg0: String, arg1: Object[]): String[];

        readPassword(): String[];

        flush(): void;

        charset(): Charset;
    }

    export interface DataInput {

        readFully(arg0: number[]): void;

        readFully(arg0: number[], arg1: number, arg2: number): void;

        skipBytes(arg0: number): number;

        readBoolean(): boolean;

        readByte(): number;

        readUnsignedByte(): number;

        readShort(): number;

        readUnsignedShort(): number;

        readChar(): String;

        readInt(): number;

        readLong(): number;

        readFloat(): number;

        readDouble(): number;

        readLine(): String;

        readUTF(): String;
    }

    export class DataInputStream extends FilterInputStream implements DataInput {
        constructor(arg0: InputStream);

        read(arg0: number[]): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        readFully(arg0: number[]): void;

        readFully(arg0: number[], arg1: number, arg2: number): void;

        skipBytes(arg0: number): number;

        readBoolean(): boolean;

        readByte(): number;

        readUnsignedByte(): number;

        readShort(): number;

        readUnsignedShort(): number;

        readChar(): String;

        readInt(): number;

        readLong(): number;

        readFloat(): number;

        readDouble(): number;

        readLine(): String;

        readUTF(): String;

        static readUTF(arg0: DataInput): String;
    }

    export interface DataOutput {

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        writeBoolean(arg0: boolean): void;

        writeByte(arg0: number): void;

        writeShort(arg0: number): void;

        writeChar(arg0: number): void;

        writeInt(arg0: number): void;

        writeLong(arg0: number): void;

        writeFloat(arg0: number): void;

        writeDouble(arg0: number): void;

        writeBytes(arg0: String): void;

        writeChars(arg0: String): void;

        writeUTF(arg0: String): void;
    }

    export class DataOutputStream extends FilterOutputStream implements DataOutput {
        constructor(arg0: OutputStream);

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        writeBoolean(arg0: boolean): void;

        writeByte(arg0: number): void;

        writeShort(arg0: number): void;

        writeChar(arg0: number): void;

        writeInt(arg0: number): void;

        writeLong(arg0: number): void;

        writeFloat(arg0: number): void;

        writeDouble(arg0: number): void;

        writeBytes(arg0: String): void;

        writeChars(arg0: String): void;

        writeUTF(arg0: String): void;

        size(): number;
    }

    export class EOFException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export interface Externalizable extends Serializable {

        writeExternal(arg0: ObjectOutput): void;

        readExternal(arg0: ObjectInput): void;
    }

    export class File extends Object implements Serializable, Comparable<File> {
        static separatorChar: String
        static separator: String
        static pathSeparatorChar: String
        static pathSeparator: String
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: File, arg1: String);
        constructor(arg0: URI);

        getName(): String;

        getParent(): String;

        getParentFile(): File;

        getPath(): String;

        isAbsolute(): boolean;

        getAbsolutePath(): String;

        getAbsoluteFile(): File;

        getCanonicalPath(): String;

        getCanonicalFile(): File;

        toURL(): URL;

        toURI(): URI;

        canRead(): boolean;

        canWrite(): boolean;

        exists(): boolean;

        isDirectory(): boolean;

        isFile(): boolean;

        isHidden(): boolean;

        lastModified(): number;

        length(): number;

        createNewFile(): boolean;

        delete(): boolean;

        deleteOnExit(): void;

        list(): String[];

        list(arg0: FilenameFilter): String[];

        listFiles(): File[];

        listFiles(arg0: FilenameFilter): File[];

        listFiles(arg0: FileFilter): File[];

        mkdir(): boolean;

        mkdirs(): boolean;

        renameTo(arg0: File): boolean;

        setLastModified(arg0: number): boolean;

        setReadOnly(): boolean;

        setWritable(arg0: boolean, arg1: boolean): boolean;

        setWritable(arg0: boolean): boolean;

        setReadable(arg0: boolean, arg1: boolean): boolean;

        setReadable(arg0: boolean): boolean;

        setExecutable(arg0: boolean, arg1: boolean): boolean;

        setExecutable(arg0: boolean): boolean;

        canExecute(): boolean;

        static listRoots(): File[];

        getTotalSpace(): number;

        getFreeSpace(): number;

        getUsableSpace(): number;

        static createTempFile(arg0: String, arg1: String, arg2: File): File;

        static createTempFile(arg0: String, arg1: String): File;

        compareTo(arg0: File): number;

        equals(arg0: Object): boolean;

        hashCode(): number;
        toString(): string;

        toPath(): Path;
    }

    export class FileDescriptor {
        static in: FileDescriptor
        static out: FileDescriptor
        static err: FileDescriptor
        constructor();

        valid(): boolean;

        sync(): void;
    }

    export interface FileFilter {

        accept(arg0: File): boolean;
    }

    export class FileInputStream extends InputStream {
        constructor(arg0: String);
        constructor(arg0: File);
        constructor(arg0: FileDescriptor);

        read(): number;

        read(arg0: number[]): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        readAllBytes(): number[];

        readNBytes(arg0: number): number[];

        skip(arg0: number): number;

        available(): number;

        close(): void;

        getFD(): FileDescriptor;

        getChannel(): FileChannel;
    }

    export class FileNotFoundException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export class FileOutputStream extends OutputStream {
        constructor(arg0: String);
        constructor(arg0: String, arg1: boolean);
        constructor(arg0: File);
        constructor(arg0: File, arg1: boolean);
        constructor(arg0: FileDescriptor);

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        close(): void;

        getFD(): FileDescriptor;

        getChannel(): FileChannel;
    }

    export class FilePermission extends Permission implements Serializable {
        constructor(arg0: String, arg1: String);

        implies(arg0: Permission): boolean;

        equals(arg0: Object): boolean;

        hashCode(): number;

        getActions(): String;

        newPermissionCollection(): PermissionCollection;
    }

    export class FileReader extends InputStreamReader {
        constructor(arg0: String);
        constructor(arg0: File);
        constructor(arg0: FileDescriptor);
        constructor(arg0: String, arg1: Charset);
        constructor(arg0: File, arg1: Charset);
    }

    export class FileWriter extends OutputStreamWriter {
        constructor(arg0: String);
        constructor(arg0: String, arg1: boolean);
        constructor(arg0: File);
        constructor(arg0: File, arg1: boolean);
        constructor(arg0: FileDescriptor);
        constructor(arg0: String, arg1: Charset);
        constructor(arg0: String, arg1: Charset, arg2: boolean);
        constructor(arg0: File, arg1: Charset);
        constructor(arg0: File, arg1: Charset, arg2: boolean);
    }

    export interface FilenameFilter {

        accept(arg0: File, arg1: String): boolean;
    }

    export class FilterInputStream extends InputStream {

        read(): number;

        read(arg0: number[]): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        available(): number;

        close(): void;

        mark(arg0: number): void;

        reset(): void;

        markSupported(): boolean;
    }

    export class FilterOutputStream extends OutputStream {
        constructor(arg0: OutputStream);

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export abstract class FilterReader extends Reader {

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        ready(): boolean;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;
    }

    export abstract class FilterWriter extends Writer {

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String, arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export interface Flushable {

        flush(): void;
    }

    export class IOError extends Error {
        constructor(arg0: Throwable);
    }

    export class IOException extends Exception {
        constructor();
        constructor(arg0: String);
        constructor(arg0: String, arg1: Throwable);
        constructor(arg0: Throwable);
    }

    export abstract class InputStream implements Closeable {
        constructor();

        static nullInputStream(): InputStream;

        abstract read(): number;

        read(arg0: number[]): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        readAllBytes(): number[];

        readNBytes(arg0: number): number[];

        readNBytes(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        skipNBytes(arg0: number): void;

        available(): number;

        close(): void;

        mark(arg0: number): void;

        reset(): void;

        markSupported(): boolean;

        transferTo(arg0: OutputStream): number;
    }

    export class InputStreamReader extends Reader {
        constructor(arg0: InputStream);
        constructor(arg0: InputStream, arg1: String);
        constructor(arg0: InputStream, arg1: Charset);
        constructor(arg0: InputStream, arg1: CharsetDecoder);

        getEncoding(): String;

        read(arg0: CharBuffer): number;

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        ready(): boolean;

        close(): void;
    }

    export class InterruptedIOException extends IOException {
        bytesTransferred: number
        constructor();
        constructor(arg0: String);
    }

    export class InvalidClassException extends ObjectStreamException {
        classname: String
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);

        getMessage(): String;
    }

    export class InvalidObjectException extends ObjectStreamException {
        constructor(arg0: String);
    }

    export class LineNumberInputStream extends FilterInputStream {
        constructor(arg0: InputStream);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        setLineNumber(arg0: number): void;

        getLineNumber(): number;

        available(): number;

        mark(arg0: number): void;

        reset(): void;
    }

    export class LineNumberReader extends BufferedReader {
        constructor(arg0: Reader);
        constructor(arg0: Reader, arg1: number);

        setLineNumber(arg0: number): void;

        getLineNumber(): number;

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        readLine(): String;

        skip(arg0: number): number;

        mark(arg0: number): void;

        reset(): void;
    }

    export class NotActiveException extends ObjectStreamException {
        constructor(arg0: String);
        constructor();
    }

    export class NotSerializableException extends ObjectStreamException {
        constructor(arg0: String);
        constructor();
    }

    export interface ObjectInput extends DataInput, AutoCloseable {

        readObject(): Object;

        read(): number;

        read(arg0: number[]): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        available(): number;

        close(): void;
    }

    export namespace ObjectInputFilter {
        function
/* default */ allowFilter(arg0: Predicate<Class<any>>, arg1: ObjectInputFilter.Status): ObjectInputFilter;
        function
/* default */ rejectFilter(arg0: Predicate<Class<any>>, arg1: ObjectInputFilter.Status): ObjectInputFilter;
        function
/* default */ merge(arg0: ObjectInputFilter, arg1: ObjectInputFilter): ObjectInputFilter;
        function
/* default */ rejectUndecidedClass(arg0: ObjectInputFilter): ObjectInputFilter;
    }

    export interface ObjectInputFilter {

        checkInput(arg0: ObjectInputFilter.FilterInfo): ObjectInputFilter.Status;
    }
    export namespace ObjectInputFilter {
        export class Config {

            static getSerialFilter(): ObjectInputFilter;

            static setSerialFilter(arg0: ObjectInputFilter): void;

            static getSerialFilterFactory(): BinaryOperator<ObjectInputFilter>;

            static setSerialFilterFactory(arg0: BinaryOperator<ObjectInputFilter>): void;

            static createFilter(arg0: String): ObjectInputFilter;
        }

        export interface FilterInfo {

            serialClass(): Class<any>;

            arrayLength(): number;

            depth(): number;

            references(): number;

            streamBytes(): number;
        }

        export class Status extends Enum<ObjectInputFilter.Status> {
            static UNDECIDED: ObjectInputFilter.Status
            static ALLOWED: ObjectInputFilter.Status
            static REJECTED: ObjectInputFilter.Status

            static values(): ObjectInputFilter.Status[];

            static valueOf(arg0: String): ObjectInputFilter.Status;
            /**
            * DO NOT USE
            */
            static valueOf<T extends Enum<T>>(arg0: Class<T>, arg1: String): T;
        }

    }

    export class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
        constructor(arg0: InputStream);

        readObject(): Object;

        readUnshared(): Object;

        defaultReadObject(): void;

        readFields(): ObjectInputStream.GetField;

        registerValidation(arg0: ObjectInputValidation, arg1: number): void;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        available(): number;

        close(): void;

        readBoolean(): boolean;

        readByte(): number;

        readUnsignedByte(): number;

        readChar(): String;

        readShort(): number;

        readUnsignedShort(): number;

        readInt(): number;

        readLong(): number;

        readFloat(): number;

        readDouble(): number;

        readFully(arg0: number[]): void;

        readFully(arg0: number[], arg1: number, arg2: number): void;

        skipBytes(arg0: number): number;

        readLine(): String;

        readUTF(): String;

        getObjectInputFilter(): ObjectInputFilter;

        setObjectInputFilter(arg0: ObjectInputFilter): void;
    }
    export namespace ObjectInputStream {
        export abstract class GetField {
            constructor();

            abstract getObjectStreamClass(): ObjectStreamClass;

            abstract defaulted(arg0: String): boolean;

            abstract get(arg0: String, arg1: boolean): boolean;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: String): String;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: number): number;

            abstract get(arg0: String, arg1: Object): Object;
        }

    }

    export interface ObjectInputValidation {

        validateObject(): void;
    }

    export interface ObjectOutput extends DataOutput, AutoCloseable {

        writeObject(arg0: Object): void;

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {
        constructor(arg0: OutputStream);

        useProtocolVersion(arg0: number): void;

        writeObject(arg0: Object): void;

        writeUnshared(arg0: Object): void;

        defaultWriteObject(): void;

        putFields(): ObjectOutputStream.PutField;

        writeFields(): void;

        reset(): void;

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;

        writeBoolean(arg0: boolean): void;

        writeByte(arg0: number): void;

        writeShort(arg0: number): void;

        writeChar(arg0: number): void;

        writeInt(arg0: number): void;

        writeLong(arg0: number): void;

        writeFloat(arg0: number): void;

        writeDouble(arg0: number): void;

        writeBytes(arg0: String): void;

        writeChars(arg0: String): void;

        writeUTF(arg0: String): void;
    }
    export namespace ObjectOutputStream {
        export abstract class PutField {
            constructor();

            abstract put(arg0: String, arg1: boolean): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: String): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: number): void;

            abstract put(arg0: String, arg1: Object): void;

            abstract write(arg0: ObjectOutput): void;
        }

    }

    export class ObjectStreamClass implements Serializable {
        static NO_FIELDS: ObjectStreamField[]

        static lookup(arg0: Class<any>): ObjectStreamClass;

        static lookupAny(arg0: Class<any>): ObjectStreamClass;

        getName(): String;

        getSerialVersionUID(): number;

        forClass(): Class<any>;

        getFields(): ObjectStreamField[];

        getField(arg0: String): ObjectStreamField;
        toString(): string;
    }

    export namespace ObjectStreamConstants {
        const STREAM_MAGIC: number
        const STREAM_VERSION: number
        const TC_BASE: number
        const TC_NULL: number
        const TC_REFERENCE: number
        const TC_CLASSDESC: number
        const TC_OBJECT: number
        const TC_STRING: number
        const TC_ARRAY: number
        const TC_CLASS: number
        const TC_BLOCKDATA: number
        const TC_ENDBLOCKDATA: number
        const TC_RESET: number
        const TC_BLOCKDATALONG: number
        const TC_EXCEPTION: number
        const TC_LONGSTRING: number
        const TC_PROXYCLASSDESC: number
        const TC_ENUM: number
        const TC_MAX: number
        const baseWireHandle: number
        const SC_WRITE_METHOD: number
        const SC_BLOCK_DATA: number
        const SC_SERIALIZABLE: number
        const SC_EXTERNALIZABLE: number
        const SC_ENUM: number
        const SUBSTITUTION_PERMISSION: SerializablePermission
        const SUBCLASS_IMPLEMENTATION_PERMISSION: SerializablePermission
        const SERIAL_FILTER_PERMISSION: SerializablePermission
        const PROTOCOL_VERSION_1: number
        const PROTOCOL_VERSION_2: number
    }

    export interface ObjectStreamConstants {
        STREAM_MAGIC: number
        STREAM_VERSION: number
        TC_BASE: number
        TC_NULL: number
        TC_REFERENCE: number
        TC_CLASSDESC: number
        TC_OBJECT: number
        TC_STRING: number
        TC_ARRAY: number
        TC_CLASS: number
        TC_BLOCKDATA: number
        TC_ENDBLOCKDATA: number
        TC_RESET: number
        TC_BLOCKDATALONG: number
        TC_EXCEPTION: number
        TC_LONGSTRING: number
        TC_PROXYCLASSDESC: number
        TC_ENUM: number
        TC_MAX: number
        baseWireHandle: number
        SC_WRITE_METHOD: number
        SC_BLOCK_DATA: number
        SC_SERIALIZABLE: number
        SC_EXTERNALIZABLE: number
        SC_ENUM: number
        SUBSTITUTION_PERMISSION: SerializablePermission
        SUBCLASS_IMPLEMENTATION_PERMISSION: SerializablePermission
        SERIAL_FILTER_PERMISSION: SerializablePermission
        PROTOCOL_VERSION_1: number
        PROTOCOL_VERSION_2: number
    }

    export abstract class ObjectStreamException extends IOException {
    }

    export class ObjectStreamField extends Object implements Comparable<Object> {
        constructor(arg0: String, arg1: Class<any>);
        constructor(arg0: String, arg1: Class<any>, arg2: boolean);

        getName(): String;

        getType(): Class<any>;

        getTypeCode(): String;

        getTypeString(): String;

        getOffset(): number;

        isPrimitive(): boolean;

        isUnshared(): boolean;

        compareTo(arg0: Object): number;
        toString(): string;
    }

    export class OptionalDataException extends ObjectStreamException {
        length: number
        eof: boolean
    }

    export abstract class OutputStream implements Closeable, Flushable {
        constructor();

        static nullOutputStream(): OutputStream;

        abstract write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export class OutputStreamWriter extends Writer {
        constructor(arg0: OutputStream, arg1: String);
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: Charset);
        constructor(arg0: OutputStream, arg1: CharsetEncoder);

        getEncoding(): String;

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String, arg1: number, arg2: number): void;

        append(arg0: CharSequence, arg1: number, arg2: number): Writer;

        append(arg0: CharSequence): Writer;

        flush(): void;

        close(): void;
    }

    export class PipedInputStream extends InputStream {
        constructor(arg0: PipedOutputStream);
        constructor(arg0: PipedOutputStream, arg1: number);
        constructor();
        constructor(arg0: number);

        connect(arg0: PipedOutputStream): void;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        available(): number;

        close(): void;
    }

    export class PipedOutputStream extends OutputStream {
        constructor(arg0: PipedInputStream);
        constructor();

        connect(arg0: PipedInputStream): void;

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export class PipedReader extends Reader {
        constructor(arg0: PipedWriter);
        constructor(arg0: PipedWriter, arg1: number);
        constructor();
        constructor(arg0: number);

        connect(arg0: PipedWriter): void;

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        ready(): boolean;

        close(): void;
    }

    export class PipedWriter extends Writer {
        constructor(arg0: PipedReader);
        constructor();

        connect(arg0: PipedReader): void;

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        flush(): void;

        close(): void;
    }

    export class PrintStream extends FilterOutputStream implements Appendable, Closeable {
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: boolean);
        constructor(arg0: OutputStream, arg1: boolean, arg2: String);
        constructor(arg0: OutputStream, arg1: boolean, arg2: Charset);
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: Charset);
        constructor(arg0: File);
        constructor(arg0: File, arg1: String);
        constructor(arg0: File, arg1: Charset);

        flush(): void;

        close(): void;

        checkError(): boolean;

        write(arg0: number): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        write(arg0: number[]): void;

        writeBytes(arg0: number[]): void;

        print(arg0: boolean): void;

        print(arg0: String): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: String[]): void;

        print(arg0: String): void;

        print(arg0: Object): void;

        println(): void;

        println(arg0: boolean): void;

        println(arg0: String): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: String[]): void;

        println(arg0: String): void;

        println(arg0: Object): void;

        printf(arg0: String, arg1: Object[]): PrintStream;

        printf(arg0: Locale, arg1: String, arg2: Object[]): PrintStream;

        format(arg0: String, arg1: Object[]): PrintStream;

        format(arg0: Locale, arg1: String, arg2: Object[]): PrintStream;

        append(arg0: CharSequence): PrintStream;

        append(arg0: CharSequence, arg1: number, arg2: number): PrintStream;

        append(arg0: String): PrintStream;
    }

    export class PrintWriter extends Writer {
        constructor(arg0: Writer);
        constructor(arg0: Writer, arg1: boolean);
        constructor(arg0: OutputStream);
        constructor(arg0: OutputStream, arg1: boolean);
        constructor(arg0: OutputStream, arg1: boolean, arg2: Charset);
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
        constructor(arg0: String, arg1: Charset);
        constructor(arg0: File);
        constructor(arg0: File, arg1: String);
        constructor(arg0: File, arg1: Charset);

        flush(): void;

        close(): void;

        checkError(): boolean;

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String[]): void;

        write(arg0: String, arg1: number, arg2: number): void;

        write(arg0: String): void;

        print(arg0: boolean): void;

        print(arg0: String): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: number): void;

        print(arg0: String[]): void;

        print(arg0: String): void;

        print(arg0: Object): void;

        println(): void;

        println(arg0: boolean): void;

        println(arg0: String): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: number): void;

        println(arg0: String[]): void;

        println(arg0: String): void;

        println(arg0: Object): void;

        printf(arg0: String, arg1: Object[]): PrintWriter;

        printf(arg0: Locale, arg1: String, arg2: Object[]): PrintWriter;

        format(arg0: String, arg1: Object[]): PrintWriter;

        format(arg0: Locale, arg1: String, arg2: Object[]): PrintWriter;

        append(arg0: CharSequence): PrintWriter;

        append(arg0: CharSequence, arg1: number, arg2: number): PrintWriter;

        append(arg0: String): PrintWriter;
    }

    export class PushbackInputStream extends FilterInputStream {
        constructor(arg0: InputStream, arg1: number);
        constructor(arg0: InputStream);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        unread(arg0: number): void;

        unread(arg0: number[], arg1: number, arg2: number): void;

        unread(arg0: number[]): void;

        available(): number;

        skip(arg0: number): number;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;
    }

    export class PushbackReader extends FilterReader {
        constructor(arg0: Reader, arg1: number);
        constructor(arg0: Reader);

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        unread(arg0: number): void;

        unread(arg0: String[], arg1: number, arg2: number): void;

        unread(arg0: String[]): void;

        ready(): boolean;

        mark(arg0: number): void;

        reset(): void;

        markSupported(): boolean;

        close(): void;

        skip(arg0: number): number;
    }

    export class RandomAccessFile implements DataOutput, DataInput, Closeable {
        constructor(arg0: String, arg1: String);
        constructor(arg0: File, arg1: String);

        getFD(): FileDescriptor;

        getChannel(): FileChannel;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        read(arg0: number[]): number;

        readFully(arg0: number[]): void;

        readFully(arg0: number[], arg1: number, arg2: number): void;

        skipBytes(arg0: number): number;

        write(arg0: number): void;

        write(arg0: number[]): void;

        write(arg0: number[], arg1: number, arg2: number): void;

        getFilePointer(): number;

        seek(arg0: number): void;

        length(): number;

        setLength(arg0: number): void;

        close(): void;

        readBoolean(): boolean;

        readByte(): number;

        readUnsignedByte(): number;

        readShort(): number;

        readUnsignedShort(): number;

        readChar(): String;

        readInt(): number;

        readLong(): number;

        readFloat(): number;

        readDouble(): number;

        readLine(): String;

        readUTF(): String;

        writeBoolean(arg0: boolean): void;

        writeByte(arg0: number): void;

        writeShort(arg0: number): void;

        writeChar(arg0: number): void;

        writeInt(arg0: number): void;

        writeLong(arg0: number): void;

        writeFloat(arg0: number): void;

        writeDouble(arg0: number): void;

        writeBytes(arg0: String): void;

        writeChars(arg0: String): void;

        writeUTF(arg0: String): void;
    }

    export abstract class Reader implements Readable, Closeable {

        static nullReader(): Reader;

        read(arg0: CharBuffer): number;

        read(): number;

        read(arg0: String[]): number;

        abstract read(arg0: String[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        ready(): boolean;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        abstract close(): void;

        transferTo(arg0: Writer): number;
    }

    export class SequenceInputStream extends InputStream {
        constructor(arg0: Enumeration<InputStream>);
        constructor(arg0: InputStream, arg1: InputStream);

        available(): number;

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        close(): void;
    }


    export interface Serializable {
    }

    export class SerializablePermission extends BasicPermission {
        constructor(arg0: String);
        constructor(arg0: String, arg1: String);
    }

    export class StreamCorruptedException extends ObjectStreamException {
        constructor(arg0: String);
        constructor();
    }

    export class StreamTokenizer {
        ttype: number
        static TT_EOF: number
        static TT_EOL: number
        static TT_NUMBER: number
        static TT_WORD: number
        sval: String
        nval: number
        constructor(arg0: InputStream);
        constructor(arg0: Reader);

        resetSyntax(): void;

        wordChars(arg0: number, arg1: number): void;

        whitespaceChars(arg0: number, arg1: number): void;

        ordinaryChars(arg0: number, arg1: number): void;

        ordinaryChar(arg0: number): void;

        commentChar(arg0: number): void;

        quoteChar(arg0: number): void;

        parseNumbers(): void;

        eolIsSignificant(arg0: boolean): void;

        slashStarComments(arg0: boolean): void;

        slashSlashComments(arg0: boolean): void;

        lowerCaseMode(arg0: boolean): void;

        nextToken(): number;

        pushBack(): void;

        lineno(): number;
        toString(): string;
    }

    export class StringBufferInputStream extends InputStream {
        constructor(arg0: String);

        read(): number;

        read(arg0: number[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        available(): number;

        reset(): void;
    }

    export class StringReader extends Reader {
        constructor(arg0: String);

        read(): number;

        read(arg0: String[], arg1: number, arg2: number): number;

        skip(arg0: number): number;

        ready(): boolean;

        markSupported(): boolean;

        mark(arg0: number): void;

        reset(): void;

        close(): void;
    }

    export class StringWriter extends Writer {
        constructor();
        constructor(arg0: number);

        write(arg0: number): void;

        write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String): void;

        write(arg0: String, arg1: number, arg2: number): void;

        append(arg0: CharSequence): StringWriter;

        append(arg0: CharSequence, arg1: number, arg2: number): StringWriter;

        append(arg0: String): StringWriter;
        toString(): string;

        getBuffer(): StringBuffer;

        flush(): void;

        close(): void;
    }

    export class SyncFailedException extends IOException {
        constructor(arg0: String);
    }

    export class UTFDataFormatException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export class UncheckedIOException extends RuntimeException {
        constructor(arg0: String, arg1: IOException);
        constructor(arg0: IOException);

        getCause(): IOException;
    }

    export class UnsupportedEncodingException extends IOException {
        constructor();
        constructor(arg0: String);
    }

    export class WriteAbortedException extends ObjectStreamException {
        detail: Exception
        constructor(arg0: String, arg1: Exception);

        getMessage(): String;

        getCause(): Throwable;
    }

    export abstract class Writer implements Appendable, Closeable, Flushable {

        static nullWriter(): Writer;

        write(arg0: number): void;

        write(arg0: String[]): void;

        abstract write(arg0: String[], arg1: number, arg2: number): void;

        write(arg0: String): void;

        write(arg0: String, arg1: number, arg2: number): void;

        append(arg0: CharSequence): Writer;

        append(arg0: CharSequence, arg1: number, arg2: number): Writer;

        append(arg0: String): Writer;

        abstract flush(): void;

        abstract close(): void;
    }

}


`;