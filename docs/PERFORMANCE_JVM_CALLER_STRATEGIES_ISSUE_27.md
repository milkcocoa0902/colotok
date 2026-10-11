# Issue #27: JVM の caller 取得方式の比較

測定日: 2026-10-11（JST）  
対象: caller の中間 allocation 最適化後の作業ツリー（base commit `16b458250ea688b3486b3d4ac57a2fa909111644`）  
関連: [前段の allocation 最適化](PERFORMANCE_CALLER_ALLOCATION_ISSUE_27.md)

追記: ユーザー指示により [StackWalker＋Iterator を本番 JVM actual へ実装し、正式な jar 同士で再測定](PERFORMANCE_JVM_STACKWALKER_IMPLEMENTATION_ISSUE_27.md)した。以下は採用判断前の比較記録。

## 結果と推奨

**JVM の次の実装候補は StackWalker＋Iterator とするのが妥当。** 共有 StackWalker で既存の logging frame 区間を読み飛ばし、最初の caller で探索を終了する。取得時点・caller 情報・表記は維持する。

本番の ThreadWrapper はこの比較では変更していない。以下は実験用 adapter jar の結果であり、正式な実装採用は別途判断する。

| 方式 | 追加0 µs/件 | 追加16 µs/件 | 追加0 B/件 | 追加16 B/件 |
|---|---:|---:|---:|---:|
| 現在の最適化済み ThreadWrapper | 6.891 | 11.112 | 2360.0 | 3960.1 |
| Thread 配列（adapter control） | 7.090 | 11.179 | 2424.0 | 4024.1 |
| Throwable 配列 | 6.820 | 10.898 | 2360.0 | 3960.1 |
| StackWalker＋Stream | 2.293 | 2.397 | 1544.0 | 1584.0 |
| StackWalker＋Iterator | 2.102 | 2.169 | 1312.0 | 1336.0 |
| StackWalker＋Iterator＋ClassValue | 2.034 | 2.110 | 1312.0 | 1352.0 |

各値は15測定（3独立 JVM × 5 iterations）の平均。PlainText 生成全体で、時刻・thread name・空 attr/MDC の snapshot と caller 文字列生成を含む。formatter / I/O は含まない。B/件は累積割り当て量で、保持メモリ量ではない。

StackWalker＋Iterator は現在の方式に対して、追加0の時間が約69.5%、割り当てが約44.4%減った。追加16では時間が約80.5%、割り当てが約66.3%減った。深い stack の費用が小さいのは、caller までの探索で終了する方式と整合する結果である。

Throwable 配列への変更は現在の方式と近く、大きな改善は確認できなかった。比較 helper を通す Thread 配列 control は、候補の adapter 呼び出し・追加フレームによる費用も含む。API 候補同士は同じ adapter を使い、current は追加 helper を含まない。

ClassValue 案は Iterator 案より平均で約3%速かったが、追加0の process 平均は Iterator 2.089〜2.117 µs、ClassValue 1.969〜2.104 µsで重なる。追加16も Iterator 2.133〜2.202 µs、ClassValue 2.064〜2.155 µsで重なる。この範囲は信頼区間ではなく、微小な勝敗の統計的判定も行っていない。RETAIN_CLASS_REFERENCE と cache の導入を正当化するほどの安定した差としては扱わず、まず単純な Iterator を優先する。

## 比較した方式

- current: 前段で最適化した Thread.currentThread().stackTrace、配列の境界選別、直接的な caller 文字列組み立て。
- thread-array: 同じ処理を比較 helper から呼ぶ control。
- throwable-array: new Throwable().getStackTrace、同じ境界選別・文字列組み立て。
- walker-stream: 共有 StackWalker、dropWhile / findFirst の Stream pipeline。
- walker-iterator: 共有 StackWalker、Stream の Iterator をループし caller で終了。
- walker-classvalue: RETAIN_CLASS_REFERENCE で Class を取得し、ClassValue<Boolean> で logging class かどうかを判定する Iterator。caller の位置は毎回取得する。

StackWalker は [Java 9以降の公開 API](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.html)。本測定は JVM 11 のサポートを対象にした。getCallerClass() だけでは必要なメソッド名・行番号が揃わないため比較対象としなかった。

[ClassValue](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassValue.html) は class に値を関連付ける公開 API を使い、独自の無制限な caller 文字列 cache は導入していない。初回の class 分類、異なる class が多数現れる負荷、loader の解放、長時間の保持メモリは未測定である。

## 同等性の検証

現在のコードと比較候補は、logging owner 名・nested class prefix・org.slf4j.* の除外、最初の logging 区間の後の caller、クラス名の省略表記・メソッド・行番号を揃えた。候補も共通 formatCallerFrame を使う。

次の6経路で、6方式（計36組）の caller 文字列が current と完全一致することを測定前に確認した。

| 経路 | 現行と全候補が返した caller |
|---|---|
| direct / 追加0 | q.b.c.CallerStrategiesFixture#direct:30 |
| direct / 追加16 | q.b.c.CallerStrategiesFixture#direct:30 |
| reflection による直接 constructor | j.i.r.NativeConstructorAccessorImpl#newInstance0:-2 |
| MethodHandle constructor | j.l.i.MethodHandle#invokeWithArguments:710 |
| org.slf4j.* bridge fixture | q.b.c.CallerStrategiesFixture#slf4jBridge:46 |
| Logger で生成し worker 処理後に参照 | q.b.c.CallerStrategiesFixture#loggerAndWorker:63 |

StackWalker のデフォルトは反射 frame を隠すため、**SHOW_REFLECT_FRAMES を明示して現行の出力を維持**した。[StackWalker.Option の仕様](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.Option.html)

反射による直接生成の現行出力自体が内部 reflection frame になる点に注意する。本検証は互換性の確認であり、すべての reflection / MethodHandle 呼び出しで利用者のソース位置を返す新しい保証ではない。反射の扱いを改善する場合は別の仕様判断として扱う。

bridge fixture は package 分類規則の確認で、実 SLF4J の実装テストとは区別する。

## 実際の module 回帰テスト

Iterator と ClassValue の両候補について、比較用 jar / helper classes をテスト実行時の classpath 先頭に配置し、以下を実行した。本番ソース・Gradle ファイルへの backend 切り替えは行っていない。

- core JVM: 133 tests（132成功、1 skipped）。直接生成、level scope、structured inline、遅延 format の caller を含む。
- coroutines JVM: 42 tests 成功。async wrapper の利用者 call site を含む。
- SLF4J 1: 16 tests、SLF4J 2: 18 tests 成功。

```sh
./gradlew -I quality/benchmarks/caller/strategies.tests.init.gradle \
  -PcallerStrategy=walker-iterator \
  :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-slf4j:test :colotok-slf4j2:test --offline
# 同じ構成で -PcallerStrategy=walker-classvalue を確認する。
```

この init script は、各 Test の classpath に比較用 jar と strategy-classes を先頭追加し、caller.strategy を指定して再実行する。ClassValue 実行の4つの test worker の class-load log で、ThreadWrapper のロード元が比較用 adapter jar、helper のロード元が strategy-classes であることを確認した。Kotlin への正式な実装移植後の API/ABI check や release consumer 検証の代替にはしない。

## 測定条件と限界

- OpenJDK 11.0.32+9（Eclipse Adoptium）、Linux x86_64、同じホストで全方式を順次実行。
- JMH 1.37、1 thread、G1 GC、heap 512 MiB、各方式/深さで3独立 JVM。
- 各 JVM は warmup 3×1秒、measurement 5×1秒。6方式×2深さ×15測定 = 180 measurements。
- INFO、固定 logger 名とメッセージ、空 attr/MDC。時刻は毎回 Clock.System.now()。
- 追加フレーム0/16はインライン禁止のアプリケーション呼び出しで、JMH 自体の frame も含む。固定の stack skip 数は使わない。
- 候補は Bytecode adapter で ThreadWrapper.traceCallPoint の本体1か所を接続した。変更された jar entry が ThreadWrapper.class のみであることを検証。adapter と helper は配布しない。
- 元 jar の他の code、record constructor、timestamp、MDC、caller string format は共通。Jar / helper source のハッシュを保存する。
- 比較中に他の benchmark やビルドを実行していない。CPU の専有・周波数固定は行っていない。
- 平均 ns/record の逆数を records/s に換算。独立した Throughput mode は測定していない。
- Cold start / cache miss、多数の異なる call site、並行 logger、JDK 17/21/25、Android/Native/JS は未測定。runtime / hidden frame / reflection policy / class-reference permission の条件が変わる場合は追加確認する。

## 再現コード・生データ

- [取得方式 helper](../quality/benchmarks/caller/JvmCallerStrategies.java)
- [測定・検証 runner](../quality/benchmarks/caller/compare_strategies.py)、[手順](../quality/benchmarks/caller/README.md)
- [集計 CSV](../quality/benchmarks/caller/results/jvm-strategies-2026-10-11/summary.csv)
- [環境・実行コマンド・ハッシュ・同等性検証結果](../quality/benchmarks/caller/results/jvm-strategies-2026-10-11/environment.json)

各フォルダーの variant-{1,2,3}.json / txt に JMH 生データ、validation-variant.txt に caller の検証結果を保存した。既存の allocation 最適化前後データは上書きしていない。
