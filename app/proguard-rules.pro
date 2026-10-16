# Android の既定ルールと依存ライブラリ同梱の consumer ルールを使う。
# パッケージ全体の keep は最適化・難読化を妨げるため、アプリ固有の反射利用が必要な場合のみ追加する。

# ML Kit は manifest の ComponentRegistrar を反射で生成する。
# firebase-components 16.1.0 の consumer ルールはクラスのみを保持し、R8 full mode ではコンストラクタが削除される。
-keepclassmembers class com.google.mlkit.** implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
