# Android / JS / Linux Kotlin/Native reference benchmarks

公開ライブラリのターゲット構成やソースを変更せず、現行コードをスナップショットして `current` と `noCaller` の測定用プロジェクトを作る。`noCaller` は `LogEventMetadata.capture()` の caller 式1か所だけを空文字列にする。Linux の `linuxX64` は Kotlin/Native の Release バイナリで、Apple SDK や Darwin は使わない。

## ビルド

リポジトリルートで実行する。Gradle 9.8 が動く JDK、Android SDK、Node.js、Linux x86_64 が必要。コンパイラ・Linux sysroot・依存ライブラリは必要に応じて取得される。

```sh
export ANDROID_HOME=/path/to/Android/Sdk
python3 quality/benchmarks/platforms/stage.py
./gradlew -p quality/benchmarks/platforms \
  :current:writeAndroidRuntime :noCaller:writeAndroidRuntime \
  :current:linkReleaseExecutableLinuxX64 :noCaller:linkReleaseExecutableLinuxX64 \
  :current:jsProductionExecutableCompileSync :noCaller:jsProductionExecutableCompileSync
```

測定プロジェクトとソースのコピーは ignored の `build/` 配下に生成される。Kotlin/compiler・ライブラリのバージョンは `variant.gradle.kts.template` に固定している。Android は **androidMain の actual 実装**を使用し、jvmMain の代用ではない。JVM bytecode を D8 で DEX にし、端末の ART で実行する。

今回の Kotlin 2.4 は SDK 同梱の古い D8 と互換性警告が出たため、Google 公式 Maven の D8/R8 9.5.23 を使用した。runner はメタデータ互換性エラーが出た変換を拒否する。

```sh
curl -fsSL https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.5.23/r8-9.5.23.jar \
  -o quality/benchmarks/platforms/build/r8-9.5.23.jar
android emulator start Pixel_6_API_34 --headless
python3 quality/benchmarks/platforms/run.py --platform android \
  --sdk "$ANDROID_HOME" --serial emulator-5554 --prepare-android \
  --d8-jar quality/benchmarks/platforms/build/r8-9.5.23.jar
python3 quality/benchmarks/platforms/run.py --platform js
python3 quality/benchmarks/platforms/run.py --platform linuxX64
```

端末の `/data/local/tmp/colotok-benchmark/` に比較用 DEX を配置する。Android は `app_process` の独立 ART プロセス（heap 64〜256 MiB）を起動する。APK／Jetpack Microbenchmark の実行ではなく、AOT、R8 shrinking、前景 Activity、thermal control は含まない。エミュレーター結果は実機の速度を示さない。作業後にエミュレーターを止めるには `android emulator stop Pixel_6_API_34` を使う。

`--forks`（既定3）、`--window-ms`（既定400）、`--case`（既定all）、`--label`（既定reference）で反復と保存先を変えられる。同じ label と platform の再実行は保存済み結果を上書きする。`--case record --forks 1 --window-ms 100 --label smoke` は動作確認用。

## ケース

| case | 対象 | 単位 |
|---|---|---|
| consumeOnly | 既存メッセージの取り出しと結果消費（ハーネスの目安） | operation |
| record | PlainText 生成、時刻と metadata を毎回取得 | record |
| recordAndSimple | 生成＋標準 SimpleTextFormatter | record |
| formatPlain | 生成済みレコードを標準 PlainTextFormatter で format | format |
| formatPlainCandidate | 試作品: メッセージをそのまま返す | format |
| formatSimple | 生成済みレコードを標準 SimpleTextFormatter で format | format |
| formatSimpleCandidate | 試作品: UTC 変換1回、DATE/TIME のみ生成し buildString | format |
| printOne | 生成済み約116文字のメッセージを1回出力 | logical line |
| printBatch16 | 生成済み16行の文字列を1回出力 | logical line（時間を16で除算） |

32メッセージを循環させる。formatter 単体では固定日時のレコードを事前生成し、constructor/caller の費用を除外する。試作品が標準出力と一致することを32件について測定前にチェックする。試作品は任意の formatter 設定を扱わず、メッセージ内の `%L` などを後段で再置換する現行の挙動も再現しない。公開実装への置換には仕様・互換性の検討が必要。

出力先は Android が `android.util.Log.i`（Logcat）、JS が `console.info`、Native が Kotlin `println`。JS／Native の stdout は `/dev/null` のファイル descriptor に接続し、端末描画やディスク保存を含めない。Node の出力経路は同期／非同期の性質が出力先で変わるため、端末や pipe の速度へ適用しない。

batch は組み立て済みで、バッファの組み立て時間・待ち時間は測定しない。Android の16行 batch は16個の Logcat entry と同じ意味ではない。受付側の速度を測るため、Logcat の保存・表示・ログ欠落も検証しない。

## 方法と限界

各プラットフォーム／variant で3つの独立プロセスを順次実行し、variant の順序をセットごとに反転する。1プロセス内では9ケースを固定順で実行する。各ケースの warmup は3×400 ms、measurement は5×400 ms。64回単位で経過時間を確認し、実際の経過時間と実行回数から時間を算出する。CPU 競合を避けるため本測定中にビルドや他のプラットフォームの測定を並行させない。

結果は JVM の volatile field、JS の global property、Native の AtomicReference に格納して、未使用結果の除去を防ぐ。その費用と時間確認の費用は測定値に含まれる。`consumeOnly` は最低費用の参考で、測定値から差し引かない。安定化検知付きの JMH／Jetpack Benchmark と同等の精度は主張しない。

Android の B/unit は ART の `art.gc.bytes-allocated` の差分であり、ハーネス自身の少量の割り当ても含む。JS／Native の同等な累積割り当て量は今回は測定していない（`unavailable`）。`units/s` は平均時間の逆数による換算。集計は各ケース15測定の平均と、3つのプロセス平均の最小〜最大。最小〜最大は信頼区間ではない。

`results/reference/{android,js,linuxX64}/` に生の JSONL、集計 CSV、環境・コマンド・ソースハッシュ・成果物ハッシュを保存する。`results/smoke/` は動作確認用で集計対象外。公開 API の修正、logger dispatch／Provider キューの end-to-end、FileProvider の永続バッファ、実機比較は対象外。

参考: [Android Microbenchmark](https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview)、[Node.js stdout の I/O 特性](https://nodejs.org/docs/latest-v24.x/api/process.html#a-note-on-process-io)、[Kotlin/Native Release の LTO](https://kotlinlang.org/docs/native-improving-compilation-time.html)。
