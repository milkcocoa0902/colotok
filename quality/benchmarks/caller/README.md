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
