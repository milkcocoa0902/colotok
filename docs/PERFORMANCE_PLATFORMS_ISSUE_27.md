# Issue #27: Android / JS / Linux Kotlin/Native 追試

測定日: 2026-10-10〜11（JST）  
対象 commit: `d6b24f07adec4a8ed4f9f12cfecda8d0a58f68ca`  
関連: [Issue #27](https://github.com/milkcocoa0902/colotok/issues/27)、[先行 JVM 測定](PERFORMANCE_ISSUE_27.md)

## 測定範囲

Android 14 の既存 x86_64 エミュレーター、Node.js、Linux x86_64 の **Kotlin/Native `linuxX64` Release バイナリ**を実測した。Apple SDK／Darwin は使用していない。公開ライブラリのターゲット・実装・API は変更せず、測定専用プロジェクトで現行ソースをコンパイルした。

比較は caller を毎回取得する現行ソースと、`LogEventMetadata.capture()` の caller 式1か所だけを空文字列にしたソース。加えて、生成済みレコードの formatter 単体と、文字列を出力する backend 単体を測った。formatter の試作品は通常のメッセージに限定した改善余地の比較で、公開実装を置き換える完成品ではない。

## 環境と方法

| 対象 | 実行環境 |
|---|---|
| Android | Pixel_6_API_34 AVD、Android 14 / API 34 / x86_64、KVM、4 virtual cores、RAM 2 GiB、ART `app_process`、heap 64〜256 MiB |
| JS | Node.js 24.16.0、Kotlin/JS production executable |
| Native | Linux x86_64、Kotlin/Native 2.4.21、Release（optimized=true、debuggable=false、既定 GC） |
| ホスト | Linux、Intel Core i7-8700K、6 cores / 12 threads |
| ライブラリ | Kotlin 2.4.21、coroutines 1.11.0、datetime 0.8.0、serialization 1.11.0、Okio 3.17.0 |
| Android DEX | Google 公式 Maven の D8/R8 9.5.23、release DEX、min API 26、shrinking 無し |

Android は JVM 上の host test ではなく、**androidMain の actual 実装を DEX 化して ART 上で実行**した。ADB はユーザー設定済みの mDNS/Burst disabled 状態で正常に起動した。古い SDK 同梱 D8 での互換性警告を解消し、9.5.23 では警告の無い変換を確認してから本測定を行った。

各 platform / variant で3つの独立プロセスを順次起動し、variant の順序をセットごとに反転。各プロセスで9ケースを固定順で実行し、各ケースの warmup は3×400 ms、measurement は5×400 ms。各条件15測定の平均を示す。64 operation ごとに経過時間を確認し、結果を volatile field / global property / AtomicReference に保持する。時間確認・結果消費の費用を含み、baseline は差し引かない。

Android、JS、Native の本測定は順番に行い、同時にビルドを行っていない。Android 測定後に今回起動したエミュレーターを停止し、JS／Native のホスト測定への負荷を減らした。

Android はエミュレーターの参考値であり、実機の速度を示さない。`app_process` の JIT warmup を使った独自ハーネスで、Jetpack Microbenchmark の AOT・前景化・安定化検知を使っていない。JS／Native も独自ハーネスであり、[先行 JVM の JMH 測定](PERFORMANCE_ISSUE_27.md)との絶対値の直接比較は適切でない。

## caller 取得の有無

| 対象 | ケース | 現行 µs/件 | caller 省略 µs/件 | 時間削減 |
|---|---|---:|---:|---:|
| Android（エミュレーター） | PlainText 生成 | 20.902 | 0.315 | 98.5% |
| Android（エミュレーター） | 生成＋SimpleTextFormatter | 36.054 | 12.260 | 66.0% |
| JS / Node.js | PlainText 生成 | 2.616 | 2.511 | 4.0% |
| JS / Node.js | 生成＋SimpleTextFormatter | 34.495 | 34.825 | -1.0% |
| Kotlin/Native / Linux | PlainText 生成 | 165.799 | 0.333 | 99.8% |
| Kotlin/Native / Linux | 生成＋SimpleTextFormatter | 178.418 | 7.984 | 95.5% |

Android の割り当て量は、レコード生成で **5,651 B → 265 B/件**、生成＋SimpleTextFormatter で **8,371 B → 2,969 B/件**。ART の `art.gc.bytes-allocated` の差分で、ハーネス自身の小さな割り当ても含む。JS／Native の同等な累積割り当て量は今回測定していない。

JS は元から `traceCallPoint()` が空文字列を返し、スタックを採取していない。平均の小差はあるが、生成＋format では省略側が約1%遅い。今回の結果から caller 省略による大きな改善は確認できず、JS の優先対象は formatter である。

Native の現行バイナリでは、caller の例が `kfun:quality.benchmarks.platforms#main(...)` であることを各独立プロセスで記録した。通常の Release 設定でも測定側の呼び出し位置を保持できている。この比較では非同期 Provider を介していないため、変更実装の非同期動作は別途検証が必要。

## 文字列組み立ての改善余地

**改善余地はある。特に使わない項目の計算を省く効果が大きい。**

現行 TextFormatter は format pattern にその要素が無い場合も、replace に渡す値を先に計算する。日時の UTC 変換は DATETIME / DATE / TIME の3回で、日時を全く使わない PlainTextFormatter でも実行される。attr の文字列化と MDC snapshot の再コピーも、使用有無にかかわらず行われる。

formatter 単体ではレコードを事前生成し、constructor / caller の費用を除外した。32件の通常メッセージ、固定日時 `2020-02-03T04:05:06.789Z`、INFO、空 attr / MDC を使い、全件で試作品と現行出力の一致を事前確認した。

| 対象 | formatter | 現行 µs/format | 試作品 µs/format | 現行／試作品 |
|---|---|---:|---:|---:|
| Android（エミュレーター） | Plain（メッセージのみ） | 7.552 | 0.011 | 699.0倍 |
| Android（エミュレーター） | Simple（日付・時刻・level・message） | 10.031 | 3.530 | 2.8倍 |
| JS / Node.js | Plain（メッセージのみ） | 32.330 | 0.033 | 985.3倍 |
| JS / Node.js | Simple（日付・時刻・level・message） | 33.222 | 11.298 | 2.9倍 |
| Kotlin/Native / Linux | Plain（メッセージのみ） | 6.800 | 0.017 | 400.0倍 |
| Kotlin/Native / Linux | Simple（日付・時刻・level・message） | 7.685 | 1.986 | 3.9倍 |

Plain の試作品は既存メッセージをそのまま返すため、測定値はハーネスの最低費用に近い（Android の consumeOnly は約8.6 ns、JS は約29.4 ns）。「ログ処理全体がこの速度になる」という結果ではない。

Simple の試作品は UTC 変換を1回にし、必要な DATE / TIME だけ生成して buildString で1つの出力文字列を作る。Android では Simple の割り当て量も **2,616 B → 1,149 B/format** となった。

この試作品は、メッセージ中の `%L` などを後段で再置換する現行の挙動を再現しない。任意の pattern、CUSTOM、MDC、structured message、特殊なメッセージへの適用と互換性は未検証である。表の差をそのまま公開 formatter の改善率として保証しない。

実装候補は、formatter 初期化時の token 化／必要要素の判定、日時変換の共有、必要な値だけの生成、1回の組み立てによる全文置換の繰り返し削減。MDC snapshot はログ呼び出し時点の保持を維持しつつ、formatter 側の不要な再コピーを検討する。

StructuredFormatter には、各呼び出しで JsonTransformingSerializer を作り、mask が空でも JSON tree を走査・再構築する処理もある。mask 無しの高速経路や serializer の再利用も候補だが、今回 StructuredFormatter は実測していない。

## print / 出力の改善余地

出力 backend 単体では、組み立て済みの文字列を1件ずつ渡す条件と、16行を含む組み立て済み文字列を1回渡す条件を比較した。batch は時間を16で除算した「論理行あたり」で示す。formatter、batch の組み立て、Provider queue、待ち時間は含まない。

| 対象 / 出力先 | 1行ごと µs/行 | 16行 batch µs/行 | 1行ごと／batch |
|---|---:|---:|---:|
| Android（エミュレーター） / Logcat | 7.176 | 0.849 | 8.5倍 |
| JS / Node.js / /dev/null | 1.592 | 0.180 | 8.9倍 |
| Kotlin/Native / Linux / /dev/null | 1.510 | 0.399 | 3.8倍 |

呼び出し回数をまとめることで、出力入口の費用を減らせる。ただし Android の batch は16個の Logcat entry と同じ意味ではなく、timestamp/tag/level の粒度が変わる。Logcat の保存・表示速度やログ欠落は検証していない。

JS／Native の stdout は `/dev/null` の file descriptor に接続した。端末描画、pipe、ディスク保存の費用を測っていないため、実際の console 出力速度へ適用しない。Node stdout は出力先で同期／非同期の性質が変わる（[Node.js の公式 I/O 説明](https://nodejs.org/docs/latest-v24.x/api/process.html#a-note-on-process-io)）。

FileProvider は1件ごとに exists/metadata の確認、appendingSink の open、write、flush、close と rotation 判定を行う。StreamProvider も1件ごとに Sink の生成、buffer、UTF-8 byte array 生成、flush、close を行う。**Sink の保持、writeUtf8、バッファリング、件数／時間によるまとめ書き**は検討価値がある。これらの Provider の改善率は今回未測定で、flush・shutdown・rotation・失敗時の挙動を保つ設計と再測定が必要。

根拠ソース: [TextFormatter](../colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/formatter/details/TextFormatter.kt)、[StructuredFormatter](../colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/formatter/details/StructuredFormatter.kt)、[FileProvider](../colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/file/FileProvider.kt)、[StreamProvider](../colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/stream/StreamProvider.kt)。

## 判断と残る作業

Android と Linux Kotlin/Native でも caller 取得の費用は大きい。特に Native の生成単体は約165.8 µsから0.333 µsとなった。JVM の先行結果と合わせ、**caller を必要なときだけログ呼び出し時点で取得する変更を1.0.0前に検討する根拠が揃った**。公開コンストラクタと API／ABI の互換性、非同期 Provider の caller 保持は改善実装時に確認する。

formatter は **未使用要素を計算しない仕組みを優先して検討**する。通常のメッセージを使う Simple の限定試作品で Android 約2.8倍、JS 約2.9倍、Native 約3.9倍の差が確認できた。特に JS は caller より formatter の改善が有力である。

print／書き込みは **呼び出し回数と flush/open/close 回数の削減を検討**する。今回の出力 backend 単体ではまとめ出力に差があるが、行・entry の意味と即時性が変わるため、明示的な batch/flush 設定と lifecycle の整合性を設計してから Provider 全体で測定する。

Android 実機での Jetpack Microbenchmark 測定、Native / JS の割り当て量、非空 MDC / attr、structured logging、異なる stack depth、logger / Provider の end-to-end、出力先別 I/O は残作業。CPU 周波数固定や専有ホストの確保も行っていない。各平均と3プロセス平均の最小〜最大は CSV に保存しており、範囲は信頼区間ではない。

今回の self-check は測定データ32件の出力一致、caller 省略条件、引数・サンプル数・source snapshot の検証であり、公開 API の変更や完成した高速 formatter の機能試験ではない。

## 再現手順・生データ

- [測定コード・ビルド／実行手順](../quality/benchmarks/platforms/README.md)
- [Android 集計](../quality/benchmarks/platforms/results/reference/android/summary.csv)、[環境とコマンド](../quality/benchmarks/platforms/results/reference/android/environment.json)
- [JS 集計](../quality/benchmarks/platforms/results/reference/js/summary.csv)、[環境とコマンド](../quality/benchmarks/platforms/results/reference/js/environment.json)
- [Native 集計](../quality/benchmarks/platforms/results/reference/linuxX64/summary.csv)、[環境とコマンド](../quality/benchmarks/platforms/results/reference/linuxX64/environment.json)

各ディレクトリの `current-{1,2,3}.jsonl` と `noCaller-{1,2,3}.jsonl` に全測定と caller の例を保存した。`units/s` は時間の逆数による換算で、独立した throughput mode の値ではない。

参考: [Android Microbenchmark の warmup/AOT/安定化制御](https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview)、[Kotlin/Native Release の LTO](https://kotlinlang.org/docs/native-improving-compilation-time.html)。
