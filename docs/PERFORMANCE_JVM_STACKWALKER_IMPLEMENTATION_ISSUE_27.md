# Issue #27: JVM StackWalker＋Iterator の本番実装

実装・検証日: 2026-10-11（JST）  
対象: [Issue #27](https://github.com/milkcocoa0902/colotok/issues/27)  
base commit: `16b458250ea688b3486b3d4ac57a2fa909111644` + 作業ツリー

## 実装

ユーザーの実装指示に基づき、JVM actual の ThreadWrapper.traceCallPoint を **共有 StackWalker＋Iterator** へ切り替えた。

- caller を生成時点で取得する責務を維持する。LogRecord の情報生成を formatter/Provider の要求に依存させない。
- 共有 walker の walk 内で Iterator と局所状態を使い、最初の logging frame の連続区間の直後の caller を選び、その時点で探索を終了する。
- SHOW_REFLECT_FRAMES を指定し、現行の reflection frame の扱いを維持する。固定の skip 数は使わない。
- 既知 logging owner / nested class / org.slf4j.* の判定と、class/method/line の caller 表記を維持する。
- 全 stack の配列を取得しない。ClassValue、RETAIN_CLASS_REFERENCE、caller の遅延取得、formatter 要求による取得省略は導入しない。
- 公開 API/コンストラクタは変更しない。既存の JVM target 11 のままであり、StackWalker が使える Java 9以降という条件は追加の最小 JVM 要件にはならない。

今回の本番ソース差分は JVM の ThreadWrapper.jvm.kt のみ。Android/Native/JS、別作業の TextFormatter 差分を変更していない。

## 本番 jar 同士の性能比較

実験 adapter を介さず、切り替え直前に保存した配列方式 jar と、正式な Kotlin 実装をビルドした jar を比較した。両方とも caller を保持する。constructor / timestamp / thread name / attr / MDC は共通で、source hash でも変更が JVM actual のみであることを確認した。

| 追加フレーム | 配列 µs/件 | StackWalker µs/件 | 時間削減 | 配列 B/件 | StackWalker B/件 | 割り当て削減 |
|---|---:|---:|---:|---:|---:|---:|
| 0 | 7.104 | 2.159 | 69.6% | 2360.0 | 1328.0 | 43.7% |
| 16 | 11.022 | 2.327 | 78.9% | 3960.1 | 1376.0 | 65.3% |

条件: OpenJDK 11.0.32+9、JMH 1.37、Linux x86_64、1 thread、G1、heap 512 MiB。各深さ/方式で3独立 JVM、warmup 3×1秒、measurement 5×1秒、15測定の平均。INFO、固定の logger 名/メッセージ、空 attr/MDC、時刻を毎回取得した PlainText 生成。formatter と I/O の費用は含まない。各方式を順次測定し、測定の順序を反転している。

追加フレーム0/16はインライン禁止の呼び出しで、JMH の frame は両条件に含む。B/件は累積割り当て量で保持メモリ量ではない。CPU 専有・周波数固定は行っていない。process 平均の範囲は集計 CSV に保存し、信頼区間として扱わない。ops/s は時間の逆数換算。

## 検証

- core JVM: 137 tests、136成功、1 skipped。既存の direct / delayed format / structured inline / level scope の caller テストを維持。
- 新規4件: reflection constructor、MethodHandle constructor、追加32フレームでの位置一致、4 threads / 32 calls で caller と thread name の保持を確認。
- coroutines JVM 42、Loki JVM 20、CloudWatch JVM 20、SLF4J 1 JVM 16、SLF4J 2 JVM 18 tests 成功。全 JVM モジュールの結果は253 tests、252成功、1 skipped、failure/error 0。
- JVM API、Android-KMP API、Kotlin ABI check 成功。公開面の変更無し。
- 変更した Kotlin ソースの ktlint と git diff --check 成功。別作業の TextFormatter を自動整形していない。
- Java fixture で直接/追加16/reflection/MethodHandle/org.slf4j bridge/Logger→worker の6経路を旧配列 jar と比較し、caller 文字列が完全一致。

reflection constructor の現行動作は reflection 内部の frame を返す場合がある。今回もその表記を維持し、reflection でも必ず利用者のソース位置になる保証へは変更していない。

主なコマンド:

```sh
./gradlew :colotok:jvmTest :colotok-coroutines:jvmTest \
  :colotok-slf4j:test :colotok-slf4j2:test :colotok:jvmApiCheck --offline
./gradlew test :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-loki:jvmTest \
  :colotok:androidKmpApiCheck :colotok:checkKotlinAbi --offline
python3 quality/benchmarks/caller/run.py --java-home /path/to/jdk11 \
  --baseline-jar /path/to/caller-array-baseline.jar \
  --include 'CallerBenchmark\.plainRecord$' --label stackwalker-production-2026-10-11
python3 quality/benchmarks/caller/summarize.py \
  quality/benchmarks/caller/results/stackwalker-production-2026-10-11 \
  --variants baseline optimized --expected-combinations 4
```

Android は配列方式、Native は既存の Throwable 方式、JS は空 caller のまま。StackWalker を Android/Native へ適用する変更は今回の範囲外。別 JDK、実機や異なる JVM、cold start、長時間の並行負荷、release consumer / 最終品質ゲートの再確認は別途必要。Issue を自動的に close しない。

## 資料

- [本番 JVM actual](../colotok/src/jvmMain/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapper.jvm.kt)
- [追加回帰テスト](../colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/util/ThreadWrapperStackWalkerTest.kt)
- [集計 CSV](../quality/benchmarks/caller/results/stackwalker-production-2026-10-11/summary.csv)
- [環境・jar ハッシュ・実行コマンド](../quality/benchmarks/caller/results/stackwalker-production-2026-10-11/environment.json)
- [caller 同等性の確認結果](../quality/benchmarks/caller/results/stackwalker-production-2026-10-11/caller-equivalence.json)
- [取得方式6案の先行比較](PERFORMANCE_JVM_CALLER_STRATEGIES_ISSUE_27.md)

[StackWalker の仕様](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.html)では instance の共有を許し、各 walk は呼び出しスレッドの stack を扱う。今回も共有するのは walker と不変の判定情報のみで、Iterator / 判定状態 / caller は各呼び出しに閉じている。
