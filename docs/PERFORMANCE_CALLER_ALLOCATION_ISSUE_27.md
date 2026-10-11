# Issue #27: caller を保持した生成処理の allocation 最適化

測定日: 2026-10-11（JST）  
基準 commit: `16b458250ea688b3486b3d4ac57a2fa909111644` + 測定開始時の作業ツリー  
対象: [Issue #27](https://github.com/milkcocoa0902/colotok/issues/27)

## 結果

caller の生成・取得タイミング・出力を維持して、現行の処理内部を最適化した。**JVM / Android では割り当て量と生成時間を削減できた。Native の速度差は明確ではない。**

両条件とも LogRecord の生成時点で caller を取得する。formatter/Provider の使用有無による省略、遅延取得、StackWalker への切り替えは行っていない。

| 環境・ケース | 最適化前 µs/件 | 最適化後 µs/件 | 時間削減 | 前 B/件 | 後 B/件 |
|---|---:|---:|---:|---:|---:|
| JVM plainRecord / 追加0フレーム | 8.297 | 7.096 | 14.5% | 4616.0 | 2360.0 |
| JVM plainRecord / 追加16フレーム | 12.411 | 11.238 | 9.4% | 6248.1 | 3960.1 |
| JVM metricsRecord / 追加0フレーム | 8.111 | 7.129 | 12.1% | 4640.0 | 2360.0 |
| JVM metricsRecord / 追加16フレーム | 12.158 | 11.075 | 8.9% | 6248.1 | 3960.1 |
| Android emulator / PlainText | 19.215 | 10.995 | 42.8% | 5653.7 | 2504.8 |
| Linux Kotlin/Native / PlainText | 165.940 | 164.813 | 0.7% | 未測定 | 未測定 |

JVM PlainText（追加0）の割り当て削減は約48.9%、時間削減は約14.5%。Android は約55.7%、約42.8%。追加16の JVM PlainText は約36.6%、約9.4%。スタック採取自体は維持しているため、深いスタックの費用は残る。

Native の平均差は約0.7%。プロセス平均の範囲は最適化前163.645〜167.353 µs、後163.060〜166.460 µsと重なっており、この測定から速度改善を主張しない。スタック採取と全フレームの文字列化は変更しておらず、その内部費用の配分も今回は測定していない。Native の割り当て量は未測定。

## 実装

- 共通の `firstCallerFrame` で、logging frame が現れるまでを飛ばし、その連続区間の直後の最初の caller を選ぶ。同じ境界判定を保ち、Sequence / iterator / 捕捉 lambda の連鎖を除く。
- JVM / Android は logging owner のクラス名と nested-class prefix を一度だけ生成し、配列のループで判定する。単にクラス名の prefix 全体を一致扱いにはしない。
- JVM / Android の caller 文字列は package 名の先頭文字、クラス名、メソッド、行番号を直接組み立てる。2回の split と dropLast / map / joinToString、中間文字列を省く。
- Native は owner 名と `.` / `#` の比較文字列を事前計算し、同じ substring 判定をループで行う。標準 Throwable の生成と getStackTrace は維持する。
- 新しい無制限の call-site cache や Class 参照を保持する cache は導入しない。不変の既知 owner 名だけを共有する。

LogRecord / Formatter / Provider の責務、公開コンストラクタ、記録時刻・MDC・caller snapshot は変更していない。既存の default-package 表現（先頭の `.`）、inner class の `$`、未知の行番号 `-1` も維持する。

## 比較条件

先行の「caller 取得あり vs 取得なし」測定とは異なり、**caller を保持する実装同士**を比較した。

- JVM: OpenJDK 11.0.32+9、JMH 1.37、1 thread、G1、heap 512 MiB。各 case / depth / variant は3独立 JVM、warmup 3×1秒、measurement 5×1秒。PlainText / Metrics の生成のみを測定し、formatter の時間は含めない。before jar は変更前に保存し、after と同じ依存関係を用いた。
- Android: 前回と同じ Android 14 / API 34 / x86_64 / Pixel_6_API_34 AVD、KVM、ART app_process、heap 64〜256 MiB、D8/R8 9.5.23。各 variant は3独立プロセス、warmup 3×400 ms、measurement 5×400 ms、1 thread。ART の累積割り当て差分を記録。Android 実機の速度を示さない。
- Native: Kotlin/Native 2.4.21、Linux linuxX64、Release（optimized=true / debuggable=false）。各 variant は3独立プロセス、同じ400 ms条件、1 thread。
- 各測定を順次実行し、本測定中に他の測定・ビルドを並行させていない。Android 測定後に今回起動した emulator を停止してから JVM / Native を測った。
- 値は15測定の平均。最小〜最大はプロセス平均の範囲で、信頼区間ではない。ops/s は平均時間の逆数換算。JMH と独自ハーネスの絶対値を直接比較しない。

測定開始時にはユーザーの TextFormatter 作業が既にあったため、**その変更を保ち、before / after の双方に同じ内容を含めた**。ライブラリの本番ソース差分は3つの ThreadWrapper actual と共通 CallerFrameUtils のみであることをハッシュで確認した。formatter 試作品の効果や前回の数字を今回の改善率に混ぜない。

Android は全6プロセスで `q.b.p.BenchKt#main:61` が一致した。JVM の事前検証では両方が CallerBenchmark#plainAtDepth を指す。Native も全6プロセスで非空の caller と利用者側 main の frame を記録する。Native のアドレス・offset・バイナリ名は別々にコンパイルした実行ファイル間で同一とはならない。

## 検証

- core JVM: 133 tests、132成功、1 skipped。既存の delayed format / structured inline caller と、新しい直接生成・logging owner に似たクラス名・level scope のテストを含む。
- coroutines JVM 42、Loki JVM 20、CloudWatch JVM 20、SLF4J 1 JVM 16、SLF4J 2 JVM 18 tests 成功。coroutines の user call site 回帰も確認。
- core Node: 28 tests、27成功、1 skipped。core Android host: 32 tests 成功。
- 測定用 Linux Native: 共通の境界選別 / 表記5件と、直接生成・worker処理後・level scope3件、計8 tests 成功。
- JVM API / Android-KMP API / Kotlin ABI check 成功。Apple actual を含むコンパイルと ABI 確認は成功したが、Apple runtime テストは Linux では実施していない。
- 変更した7つの Kotlin ソースの ktlint 成功。TextFormatter 等の既存の作業ツリー差分をフォーマットしていない。

主な実行コマンド:

```sh
./gradlew :colotok:jvmTest :colotok:compileAndroidMain \
  :colotok-coroutines:jvmTest :colotok-slf4j:test :colotok-slf4j2:test --offline
./gradlew :colotok:jvmApiCheck :colotok:androidKmpApiCheck --offline
./gradlew test :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-loki:jvmTest \
  :colotok:jsNodeTest :colotok:testAndroidHostTest :colotok:checkKotlinAbi --offline
./gradlew -p quality/benchmarks/platforms :noCaller:linuxX64Test --offline
```

## 再現コードと生データ

- [JVM の実行手順](../quality/benchmarks/caller/README.md)、[集計](../quality/benchmarks/caller/results/caller-allocation-2026-10-11/summary.csv)、[環境と jar ハッシュ](../quality/benchmarks/caller/results/caller-allocation-2026-10-11/environment.json)
- [Android / Native の実行手順](../quality/benchmarks/platforms/README.md)
- [Android 集計](../quality/benchmarks/platforms/results/caller-allocation-2026-10-11/android/summary.csv)、[環境とソースハッシュ](../quality/benchmarks/platforms/results/caller-allocation-2026-10-11/android/environment.json)
- [Native 集計](../quality/benchmarks/platforms/results/caller-allocation-2026-10-11/linuxX64/summary.csv)、[環境とソースハッシュ](../quality/benchmarks/platforms/results/caller-allocation-2026-10-11/linuxX64/environment.json)

生データは各フォルダーの baseline / optimized の3回分の JSON または JSONL。前回の基準結果は上書きしていない。JS の caller actual は変更しておらず、今回 JS の速度比較は行っていない。
