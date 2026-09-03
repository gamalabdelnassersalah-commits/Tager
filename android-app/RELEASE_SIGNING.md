# Tager Android production signing

Production package: `com.tager.marketplace`

Current production certificate SHA-256:

`D5:43:F1:DA:B8:BF:8F:C2:06:30:2D:96:2A:4C:E0:61:74:2E:39:5A:39:37:EE:7D:64:6A:8C:86:5D:F1:0E:C2`

The private signing key and its passwords must never be committed to this repository.

## Required APK release order

1. Build the unsigned release APK with Gradle.
2. Run `zipalign` on the unsigned APK.
3. Sign the aligned APK with Android `apksigner` using the production key.
4. Verify the signature with `apksigner verify --verbose --print-certs`.
5. Verify alignment again with `zipalign -c -P 16 -v 4`.
6. Generate SHA-256 checksums for the final signed artifacts.

Do not use `jarsigner` for the production APK. It does not provide the complete modern APK signing flow required by current Android versions.

## App Links

The deployed web application must publish `/.well-known/assetlinks.json` for package `com.tager.marketplace` and the production certificate fingerprint above before verified App Links can be relied on.
