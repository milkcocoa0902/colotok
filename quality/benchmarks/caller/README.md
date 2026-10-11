# Issue #27 caller capture benchmark

公開ライブラリの実装・API・ビルド依存関係を変更せず、現行 JVM jar と caller 取得だけを省いた比較用 jar を JMH 1.37 で比較する。

## 実行

リポジトリルートで以下を実行する。Gradle 用の JDK と測定用の JDK は別でよい。測定用には **JRE ではなく JDK 11** を指定する（パッチツールは JDK 内部 ASM を使用する）。

```sh
./gradlew -I quality/benchmarks/caller/dependencies.init.gradle :colotok:prepareCallerBenchmark --console=plain
python3 quality/benchmarks/caller/run.py --java-home /path/to/jdk11
python3 quality/benchmarks/caller/summarize.py quality/benchmarks/caller/results/jdk11
```

初回は JMH 依存関係の取得にネットワークが必要。JMH の fork 間通信にはローカルソケットが必要。`--label` で結果の保存先を変更できる。同じ label の再実行は既存結果を上書きする。`--forks`、`--warmup`、`--iterations`、`--seconds` で測定回数を指定する。

## 比較の意味

- `current`: 現行 `LogEventMetadata.capture()` をそのまま使用。
- `no-caller`: jar 内の `LogEventMetadata$Companion.capture()` にある `ThreadWrapper.traceCallPoint()` 呼び出し1か所だけを空文字列に置換。
- `PatchCaller.java` は置換数を検証し、runner は変更された jar エントリがその1クラスだけであることを検証する。比較用 jar は ignored の `build/` に保存し、配布しない。
- caller 取得を省く条件判定は含まれない。実際の改善実装で到達できる利益の目安であり、機能を満たす修正実装の測定ではない。
- caller を必要とする formatter の挙動や非同期 Provider の正しさはこの比較用 jar では保証しない。本番コードを変更する際に別途検証する。

## 測定条件

- `PlainText` 生成、`Metrics` 生成、`PlainText` 生成＋`SimpleTextFormatter` の3ケース。
- 空の attr／MDC、INFO、固定の logger 名とメッセージ。イベント時刻は毎回 `Clock.System.now()` で取得。
- 追加アプリケーションフレーム0／16。生成メソッドと再帰呼び出しは `DONT_INLINE`。JMH 自体のフレームも両条件に含まれる。
- 1スレッド、G1 GC、heap 512 MiB、各ケース3つの独立 JVM、各 JVM にウォームアップ3×1秒、測定5×1秒。
- 各 fork で両 variant を連続実行し、順序を交互に反転する。variant 間・ケース間の JVM は共有しない。
- 結果を JMH に返し、dead-code elimination を防ぐ。測定前に caller が利用者側の生成メソッドを指すこと（比較用 jar では空であること）と標準 formatter の出力を確認する。
- `ns/op` は JMH AverageTime。`ops/s` は `1e9 / 平均 ns/op` の換算値で、独立した Throughput モードの測定ではない。
- `B/op` は GC profiler の `gc.alloc.rate.norm`（割り当て量、保持メモリ量ではない）。集計は15測定の平均と、3つの fork 平均の最小〜最大を示す。範囲は信頼区間ではない。

ディスク／ネットワーク I/O、Provider キュー、複数スレッド競合、StructuredText のシリアライズ、MDC 内容による差は測定対象に含めない。CPU 周波数の固定や専有ホストの確保は行っていない。

## 保存物

`results/jdk11/` に両 variant の生の JMH JSON、テキスト出力、環境・実行コマンド・jar SHA-256、集計 CSV を保存する。`results/smoke/` は動作確認用で集計対象外。

手法の参考: [OpenJDK JMH](https://github.com/openjdk/jmh)、[GC profiler の公式サンプル](https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java)。

## caller を保持する実装同士の比較

`run.py --baseline-jar /path/to/baseline.jar` で、保存した最適化前 jar と現在の jar を比較できる。
このモードは bytecode の caller 省略パッチを適用せず、両方で caller の位置を検証する。
baseline jar は同じ依存関係・caller 以外の同じソースで、最適化前にビルドして保存する。

```sh
# 最適化後の jar / classpath を準備する。
./gradlew -I quality/benchmarks/caller/dependencies.init.gradle :colotok:prepareCallerBenchmark
python3 quality/benchmarks/caller/run.py --java-home /path/to/jdk11 \
  --baseline-jar /path/to/baseline.jar \
  --include 'CallerBenchmark\.(plainRecord|metricsRecord)$' --label caller-allocation-2026-10-11
python3 quality/benchmarks/caller/summarize.py \
  quality/benchmarks/caller/results/caller-allocation-2026-10-11 \
  --variants baseline optimized --expected-combinations 8
```

`baseline` / `optimized` の生データと jar ハッシュを保存する。基準測定 `results/jdk11/` は上書きしない。

## JVM の取得 API を比較する

`compare_strategies.py` は現在の caller-preserving JVM jar を基準に、以下の6方式を比較する。
公開ライブラリのソースは変更せず、ignored の比較用 jar で ThreadWrapper.traceCallPoint の本体1か所のみを helper へ接続する。

| variant | 方法 |
|---|---|
| current | 現在の ThreadWrapper（配列＋最適化済みループ）そのまま |
| thread-array | 比較 helper を通す Thread.currentThread().getStackTrace。adapter の追加フレーム/呼び出し費用の control |
| throwable-array | new Throwable().getStackTrace＋同じ配列ループ |
| walker-stream | 共有 StackWalker＋dropWhile / findFirst |
| walker-iterator | 共有 StackWalker＋Iterator のループ、caller で終了 |
| walker-classvalue | RETAIN_CLASS_REFERENCE＋ClassValue<Boolean> で logging class を分類し、Iterator のループ |

```sh
./gradlew -I quality/benchmarks/caller/dependencies.init.gradle :colotok:prepareCallerBenchmark
python3 quality/benchmarks/caller/compare_strategies.py --java-home /path/to/jdk11
# 動作確認だけ（JMH の測定はしない）
python3 quality/benchmarks/caller/compare_strategies.py --java-home /path/to/jdk11 \
  --validate-only --label smoke-strategies
```

すべて現在のフレーム境界・クラス名短縮・メソッド名・行番号を維持する。StackWalker は SHOW_REFLECT_FRAMES を指定し、
反射での既存出力を保つ。固定の skip 数、getCallerClass だけの取得、行番号の省略、caller の事前判定は使わない。
ClassValue に保存するのは logging class かどうかの Boolean であり、caller の呼び出し位置は毎回取得する。

測定前に、直接生成・追加16フレーム・reflection・MethodHandle・org.slf4j.* の bridge fixture・Logger→worker の6経路で、
6方式すべての caller 文字列が current と完全に一致することを検証する。bridge は分類規則の fixture で、実 SLF4J module のテストとは区別する。
反射による直接生成では current 自体が reflection 内部のフレームを返す場合があり、この検証はそれを維持する同等性検証である。

性能測定は PlainText 生成、追加フレーム0/16、1 thread、各方式3独立 JVM、warmup 3×1秒、measurement 5×1秒、G1 / heap 512 MiB。
現在の1回だけの class name split 削減等は全方式に共通の formatCallerFrame を使う。Stream / Iterator と ClassValue の差も分けて比較する。
results/jvm-strategies-2026-10-11 に生データ・集計 CSV・環境・ソース/成果物ハッシュ・validation 出力を保存する。

比較 helper の追加フレームはすべての候補にあるため、API 同士は同じ adapter で比較し、current との関係は thread-array control も確認する。
ClassValue の初回解決・大量の異なる class・class loader の解放・複数 thread・異なる JDK はこの測定に含めない。
採用時は Kotlin の実装へ移し、実際の integration と API/ABI の確認、取得方法の version 対応が別途必要。

比較用 jar を使った既存 module の回帰確認は、実行時の classpath だけを切り替えて行える。
`build/classload-<strategy>-<pid>.txt` で ThreadWrapper と helper のロード元を確認できる。

```sh
./gradlew -I quality/benchmarks/caller/strategies.tests.init.gradle \
  -PcallerStrategy=walker-iterator \
  :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-slf4j:test :colotok-slf4j2:test
# ClassValue 案は -PcallerStrategy=walker-classvalue。
```

参考: [StackWalker](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.html)、
[StackWalker.Option](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.Option.html)、
[ClassValue](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassValue.html)。
