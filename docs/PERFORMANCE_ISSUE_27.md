# Issue #27: caller 取得コストの性能試験

測定日: 2026-10-10  
対象: [Issue #27](https://github.com/milkcocoa0902/colotok/issues/27)  
対象 commit: `d6b24f07adec4a8ed4f9f12cfecda8d0a58f68ca`（現行ソース、artifact version 0.5.0）

追試: [Android / JS / Linux Kotlin/Native と formatter・出力の追加測定](PERFORMANCE_PLATFORMS_ISSUE_27.md)を2026-10-10〜11に実施した。以下は先行 JVM 測定時点の記録。

## 結論

**JVM では caller 取得の性能影響が大きく、1.0.0 前に条件付き取得を検討する根拠が得られた。**

追加アプリケーションフレーム0の条件では、`PlainText` 生成が **8.03 µs → 0.057 µs**、割り当て量が **4,632 B → 232 B** になった。標準 `SimpleTextFormatter` の文字列生成まで含めても **10.50 µs → 1.525 µs** となり、caller 取得を省く比較条件で約85.5%の時間削減が見られた。

これは caller を必要としない経路の改善余地を測った結果である。formatter の要否判定、Provider の走査、互換性を維持する実装の費用は含めておらず、修正後の速度を保証するものではない。Android／Native は実測できていないため、この結果で Issue 全体の受け入れ条件を満たしたとはしない。

## 方法と環境

本体のソース・API を変更せず、現行 JVM jar と比較用 jar を JMH 1.37 で比較した。比較用 jar は `LogEventMetadata.capture()` の `ThreadWrapper.traceCallPoint()` 呼び出し1か所を空文字列に置換した。置換数が1、変更された jar エントリが `LogEventMetadata$Companion.class` のみであることを自動検証した。

この差分には、スタックトレース採取に加えて、logging frame の除外、caller 文字列の組み立てまでの費用が含まれる。スタック採取だけの単独費用ではない。timestamp、thread name、attr、MDC snapshot、レコード型の処理は両条件で共通である。

| 項目 | 条件 |
|---|---|
| ホスト | Linux x86_64、Intel Core i7-8700K、6 cores / 12 threads |
| JVM | Eclipse Adoptium OpenJDK 11.0.32+9、64-bit HotSpot |
| ライブラリ | Kotlin 2.4.21、coroutines 1.11.0、datetime 0.8.0 |
| heap / GC | 512 MiB 固定、G1 GC |
| concurrency | 1スレッド |
| データ | INFO、固定 logger 名／メッセージ、空 attr／MDC、時刻は毎回取得 |
| 呼び出し階層 | 追加フレーム0／16、生成メソッドと再帰はインライン禁止 |
| 反復 | 各ケース／条件で独立 JVM 3つ、warmup 3×1秒、measurement 5×1秒 |
| 実行順 | 両条件を順次実行し、独立実行のセットごとに順序を反転 |
| 出力消費 | JMH にレコード／生成文字列を返す |
| 事前検証 | 現行 jar の caller が利用者側の生成メソッドを指すこと、比較用では空であること、標準 formatter 出力 |

JMH 自体の呼び出しフレームが含まれるため、「追加0」はスタック全体の深さ0を意味しない。CPU 周波数固定や専有ホストの確保は行っていない。

## 実測結果

各値は15測定（3 JVM × 5 iterations）の平均。`µs/op` は JMH AverageTime の `ns/op` から変換した。`B/op` は GC profiler の `gc.alloc.rate.norm`（割り当て量）であり、保持メモリ量ではない。

| ケース | 追加フレーム | 現行 µs/op | 取得省略 µs/op | 時間削減 | 現行 B/op | 取得省略 B/op |
|---|---:|---:|---:|---:|---:|---:|
| PlainText 生成 | 0 | 8.034 | 0.057 | 99.3% | 4,632.0 | 232.0 |
| PlainText 生成 | 16 | 12.134 | 0.093 | 99.2% | 6,264.1 | 256.0 |
| Metrics 生成 | 0 | 9.820 | 0.061 | 99.4% | 4,664.1 | 232.0 |
| Metrics 生成 | 16 | 13.712 | 0.101 | 99.3% | 6,232.1 | 256.0 |
| PlainText 生成＋SimpleTextFormatter | 0 | 10.496 | 1.525 | 85.5% | 6,936.0 | 2,527.9 |
| PlainText 生成＋SimpleTextFormatter | 16 | 16.730 | 1.808 | 89.2% | 8,523.3 | 2,551.9 |

| ケース | 追加フレーム | 現行 ops/s（換算） | 取得省略 ops/s（換算） |
|---|---:|---:|---:|
| PlainText 生成 | 0 | 124,472 | 17,569,988 |
| PlainText 生成 | 16 | 82,415 | 10,703,247 |
| Metrics 生成 | 0 | 101,833 | 16,289,639 |
| Metrics 生成 | 16 | 72,927 | 9,916,290 |
| PlainText 生成＋SimpleTextFormatter | 0 | 95,272 | 655,816 |
| PlainText 生成＋SimpleTextFormatter | 16 | 59,775 | 553,162 |

`ops/s` は `1e9 / 平均 ns/op` による単一スレッドの処理能力換算で、独立した Throughput モードの実測ではない。実際の Provider を通した出力スループットでもない。

追加フレーム0の PlainText 生成では、3つの JVM の平均は現行 **7.840〜8.379 µs/op**、省略 **0.056〜0.058 µs/op**。一方、追加16の生成＋format では現行 **14.370〜21.360 µs/op**、省略 **1.621〜2.029 µs/op** と揺れが大きい。これらは fork 平均の最小〜最大で、信頼区間ではない。小さな差の議論には追加測定が必要だが、今回の caller 有無の大きな差は全 fork で確認できる。

同じ条件を1万レコード/秒へ単純換算すると、PlainText 生成で約0.080秒/秒の処理時間と約42 MiB/秒の割り当て量が caller 取得に対応する差となる。これは測定値からの外挿であり、アプリケーション全体の CPU 使用率や GC 停止時間の実測ではない。

## プラットフォーム別の確認

| 対象 | 確認内容 | 今回の扱い |
|---|---|---|
| JVM | 全レコードで `Thread.currentThread().stackTrace` を採取し、選別・文字列生成 | 実測済み |
| Android | `ThreadWrapper.android.kt` は JVM 実装とファイル内容が同一 | 静的確認のみ。ADB 36.0.2 がネットワークインターフェース処理の `OSP_CHECK ... 6 vs. 4` で起動に失敗し、端末上の測定は未実施 |
| JS | `traceCallPoint()` は常に空文字列を返す | スタック採取は行われない。今回の JVM の削減量を適用できない。性能実測は未実施 |
| Native | `Throwable().getStackTrace()`、logging owner 名リストの作成、frame 選別 | 同種の費用が発生する実装と確認。設定された Apple ターゲットは Linux ホストで実行できず、実測は未実施 |

根拠となるソース: [metadata capture](../colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/LogRecord.kt)、[JVM](../colotok/src/jvmMain/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapper.jvm.kt)、[Android](../colotok/src/androidMain/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapper.android.kt)、[JS](../colotok/src/jsMain/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapper.js.kt)、[Native](../colotok/src/nativeMain/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapper.native.kt)。

## 判断と残る作業

JVM の測定から、**「影響が小さいため1.0.0では変更しない」という判断は推奨しない**。formatter が caller を必要とするかを示す仕組みを設け、必要な場合だけログ呼び出し時点で取得する改善を検討する。

改善時は、caller を使用する formatter がある場合の正しい呼び出し位置、非同期 Provider での保持、公開 LogRecord コンストラクタと API／ABI 互換性を別途確認する必要がある。今回の比較用 jar はその機能を満たす修正ではない。

測定には Provider のキュー・ディスク・ネットワーク I/O、logger の dispatch、複数スレッド競合、StructuredText のシリアライズ、非空 attr／MDC、異なる JDK を含めていない。修正実装の再測定と Android ART 実測は残作業である。

## 再現手順と生データ

測定後に `./gradlew :colotok:jvmTest --rerun --offline --console=plain` を実行し、**125 tests、124成功、1 skipped、failure/error 0** を確認した。遅延 format 前の caller 保持と StructuredText の inline overload の caller 保持テストも成功した。これは現行コードのテスト結果であり、caller を省略する比較用 jar に対する機能検証ではない。

- [測定コード・再実行手順](../quality/benchmarks/caller/README.md)
- [集計 CSV](../quality/benchmarks/caller/results/jdk11/summary.csv)
- [実行環境・コマンド・jar SHA-256](../quality/benchmarks/caller/results/jdk11/environment.json)
- [現行 fork 1](../quality/benchmarks/caller/results/jdk11/current-1.json)、[fork 2](../quality/benchmarks/caller/results/jdk11/current-2.json)、[fork 3](../quality/benchmarks/caller/results/jdk11/current-3.json)
- [取得省略 fork 1](../quality/benchmarks/caller/results/jdk11/no-caller-1.json)、[fork 2](../quality/benchmarks/caller/results/jdk11/no-caller-2.json)、[fork 3](../quality/benchmarks/caller/results/jdk11/no-caller-3.json)

JMH は [OpenJDK の benchmark harness](https://github.com/openjdk/jmh) を使用し、割り当て量の指標は [公式 profiler サンプル](https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java) に沿って読む。
