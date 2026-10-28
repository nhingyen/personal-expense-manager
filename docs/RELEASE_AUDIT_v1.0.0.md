# Final QA & Release Readiness Audit — v1.0.0 Offline-only

Ngày audit: 2026-10-27. Phạm vi: một module `:app`, application ID
`com.ngyennhi.expensemanager`, versionName `1.0.0`, versionCode `2`, minSdk 26.
Git ở lượt audit ban đầu sạch. Lượt Final Release Verification tiếp tục trên
working tree có sẵn các thay đổi TASK-014; giữ nguyên các thay đổi đó.

**Kết luận: Not Ready for v1.0.0 để phân phối.** Signed upgrade v2 → v3 đã
**PASS trên AVD API 35 riêng** trong lượt thực thi 2026-10-28, gồm bảo toàn dữ
liệu và logo mới. Runtime API 26 chưa xác minh; CI run 40648694159 thất bại vì
profile `Pixel 2` không có trên runner. Bản sửa local chưa được chạy lại trên CI;
các manual gates còn mở không được thay thế bởi PASS API 35. Xem phần kết
quả thực thi Signed Upgrade cuối báo cáo; các mục chờ ký/chưa upgrade là lịch sử.

## Phát hiện và xử lý

Không phát hiện P0 trong phạm vi đã kiểm tra. Đây không phải chứng nhận rằng mọi
thiết bị hoặc mọi tình huống runtime đều đã được kiểm thử.

| Mức | File / nguyên nhân | Xử lý |
| --- | --- | --- |
| P1 | `app/src/main/AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`, `backup_rules.xml`: manifest bật `allowBackup`, XML là template không loại trừ dữ liệu; trái với cam kết Offline-only và tài liệu TASK-013. | Đã đặt `allowBackup=false`, `fullBackupContent=false`, loại trừ cả 9 storage domains trong cloud backup/device transfer, bỏ XML template không còn dùng. Không sửa/xóa file dữ liệu cục bộ. Có regression kiểm tra manifest/rules đã đóng gói. |
| P1 | `presentation/transaction/TransactionViewModel.kt`, `TransactionFormScreen.kt`, `TransactionDetailScreen.kt`, `TransactionComponents.kt`: Back rồi mở Add trong lúc save chờ I/O đặt lại `isSaving`; cho phép đổi draft và khởi động lần ghi thứ hai. Delete cũng không loại trừ các thao tác edit/save/đổi selected record. | Đã tái hiện bằng unit test có write gate. Khóa các thao tác xung đột trong ViewModel, khóa field/Back/Edit trong UI tới khi có kết quả; lỗi ghi giữ draft và mở lại retry. |
| P1, đã đóng cho API 35 | Release-signed upgrade giữ dữ liệu cần bằng chứng. | Người dùng cung cấp signed v3 cùng certificate. Preflight v2 khớp snapshot gốc; `install -r` lên code 3, UID giữ nguyên; hai lần compare sau install/cold launch và kiểm tra UI/icon đều PASS trên AVD riêng API 35. Không suy diễn thành PASS API 26. |
| P2 | `.githooks/pre-commit`, `.githooks/pre-push`: nhánh POSIX gọi trực tiếp `./gradlew`, trong khi file được Git lưu mode `100644`; fresh checkout POSIX có thể bị Permission denied. | Đã dùng `sh ./gradlew`; Windows vẫn dùng `gradlew.bat`. Kiểm tra cú pháp và fixture POSIX xác minh gọi đúng task, truyền mã lỗi; không thực hiện commit/push. |
| P2 | `tasks/ROADMAP.md`: TASK-006 còn mang scope portfolio release, khác task Search & Filter; dòng TASK-013 bị tách khỏi bảng. | Đồng bộ scope Search & Filter, đưa audit vào TASK-014 và sửa bảng. |
| P2, gate CI mới chưa xác minh | `.github/workflows/android-ci.yml`: remote run gần nhất cũ chỉ chạy Debug/unit/lint. | Working tree hiện đã thêm unsigned Release build và job API 26 cho `OfflineFlowTest`/`WalletMigrationTest`; chưa có remote run trên cấu hình mới. Không commit/push để kích hoạt workflow. |
| P2, đã sửa test; theo dõi | `SettingsFlowTest#systemThemeTracksDeviceWhileExplicitChoicesStayFixed`: full attempt ban đầu lỗi `Failed waiting for PixelCopy!` trong lúc test đổi system night mode. | Bỏ `captureToImage()` đúng tại ca đang đổi cấu hình; thay bằng đợi xác nhận light/dark system-bar appearance do Activity áp dụng. APK test build PASS; chạy riêng **4/4 PASS**, sau đó toàn suite hiện tại **63/63 PASS trong một lượt** (527.54 giây). Giữ nguyên assert lựa chọn System/Light/Dark. Background pixel không còn được xác nhận tại ca này; test Settings khác vẫn có kiểm tra màu nền. Lỗi PixelCopy lịch sử được giữ lại; tiếp tục theo dõi. |
| P2, theo dõi | `TransactionFilterEditorTest#englishEditorSelectsInclusiveRangeAndAppliesAtLargeText`: một full attempt của lượt tiếp tục dừng vì ANR khi chuyển focus. Chưa có trace xác định nguyên nhân. | Sau AVD khởi động lại: focused class PASS 2/2 và full suite PASS 63/63, cùng source/APK/assertion. Không kết luận đã sửa ANR hay loại trừ lỗi app/harness/môi trường. |
| P2, nguyên nhân chưa kết luận | `SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt` bị kẹt trong một lượt direct full run đồng thời guest ADB ngừng phản hồi. | Logcat đã lưu từ lượt Gradle khác cho thấy ca bắt đầu 23:11:07, runner ghi `finished` 23:11:22 và ca Settings kế tiếp bắt đầu 23:11:22; không thấy ANR, FATAL EXCEPTION hay exception app. Cùng log có TalkBack `PermissionRequestActivity`, Android `GrantPermissionsActivity`, 30–110 frame bị bỏ, SurfaceSyncGroup timeout 1s và system-server monitor contention. Đây là bằng chứng nghiêng về emulator/system UI/harness pressure, chưa chứng minh nguyên nhân hay JUnit PASS. Không sửa app/test; cần chạy riêng ca trên emulator ổn định trước khi full suite. |

Chính sách backup được đối chiếu với [Android Auto Backup](https://developer.android.com/identity/data/autobackup):
`allowBackup=false` cần đi cùng extraction rules khi kiểm soát device transfer trên
Android mới. Audit này kiểm tra cấu hình/APK; chưa chạy dịch vụ backup/transfer OEM thực tế.

## Kết quả rà soát mã nguồn

- **Transaction/Search:** amount nguyên dương `Long`, category bắt buộc; ID được giữ
  khi edit, missing record và lỗi ghi được tách biệt. Search theo note/category dùng
  `Locale.ROOT`, giữ dấu; category/type/date kết hợp AND, khoảng ngày inclusive,
  giữ thứ tự date/ID. Filter không thay đổi dữ liệu hoặc tổng Home.
- **Tổng thu/chi/Statistics:** chỉ chọn đúng `YearMonth`; exact arithmetic báo tràn
  thay vì wrap. Biểu đồ theo expense category và sáu tháng lịch, có tháng rỗng,
  số tiền chính xác bằng text, tránh chia cho 0. Tên category do người dùng nhập
  không bị dịch; phần trăm và số compact trong biểu đồ có làm tròn như đã mô tả.
- **Wallet:** opening balance + thu − chi trên mọi ngày của từng ví; BigInteger
  tránh tràn trung gian, kết quả cuối/total được kiểm tra `Long`. Opening balance
  không đi vào báo cáo thu/chi hoặc ngân sách. Guarded DELETE và FK RESTRICT bảo vệ
  ví đang có giao dịch; ví 1 không bị xóa. DEX đã kiểm tra có synthetic backport cho
  `BigInteger.longValueExact`, không gọi trực tiếp API 31 trên minSdk 26.
- **Budget:** chỉ expense của tháng đã chọn tiêu hạn mức; remaining âm khi vượt,
  exact percent có thể >100%, thanh vẽ clamp. Giữ riêng từng tháng, khóa tháng/draft
  khi ghi, phân biệt unset/loading/error/stale/overflow và giữ input khi ghi thất bại.
- **Room/data:** schema 2, đủ schema export 1/2, migration 1→2 copy nguyên ID/field,
  tạo ví 1 và giữ sequence kể cả ID đã xóa. Không `fallbackToDestructiveMigration`,
  không đổi DB `expense_manager.db`, budget prefs `monthly_budgets` hay prefix
  `monthly_budget:`. DAO writes là suspend, reads Flow; preferences commit trên IO.
- **State/lifecycle/navigation:** ViewModel StateFlow bất biến, `viewModelScope`,
  cancellation được truyền tiếp, retry hủy collector cũ; Compose collect theo lifecycle.
  Tab save/restore, Home là Back root; Add/detail/edit là route phụ. Draft/filter
  sau process death vẫn có giới hạn đã công bố, khác với Activity recreation.
- **Offline/settings:** không còn Firebase Auth, Firestore, Google credential/auth
  SDK/config/plugin hoặc Backup UI trong production source/catalog/CI. Không cần
  account/network. Language dùng AppCompat/framework, theme dùng prefs riêng;
  phiên bản lấy từ package. Có 171 resource keys VI/EN trùng nhau, đúng placeholders.
- **Kiến trúc:** production domain không import Android/Compose/Room/Hilt/data/UI;
  presentation/data không phụ thuộc ngược nhau. Không thêm module/dependency.
- **Performance:** list dùng lazy/key, báo cáo tái sử dụng snapshot; tuy nhiên mỗi
  feature observe toàn bộ transaction, lọc/tổng hợp trong ViewModel trên main, và
  release tắt optimization. Chưa có benchmark dữ liệu lớn/jank hoặc performance
  budget; không coi đó là bug đã xác nhận và không refactor trong audit.

## Verification trước lượt tiếp tục

Lệnh cuối trên source sau sửa lỗi:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:signingReport --console=plain --max-workers=1 --no-daemon --no-configuration-cache
```

**BUILD SUCCESSFUL**, 12m 41s, 150 tasks (69 executed, 81 up-to-date).

| Check | Kết quả thực tế |
| --- | --- |
| `assembleDebug` | PASS; cài bằng `adb -s emulator-5554 install -r`, không uninstall/clear. |
| `assembleRelease` | PASS; tạo `app-release-unsigned.apk`, 9,246,543 bytes. Release manifest không debuggable, đúng version/min/target SDK, không INTERNET/dangerous permission. Permission duy nhất là signature permission nội bộ do AndroidX sinh. |
| Native packaging | `zipalign -c -P 16 4` PASS; mọi LOAD segment của thư viện arm64-v8a/x86_64 có ELF alignment 16 KB. Chưa chạy trên thiết bị page-size 16 KB. |
| `testDebugUnitTest` | PASS: 98 tests / 17 suites, 0 failure/error/skip. Gồm 2 test pending-write mới. |
| `lintDebug` | PASS: 0 error, 14 warnings, 1 hint; không thêm suppression/baseline. Warnings: dependency/version advisories, unused resources, redundant label; hint về boxed Compose state. |
| `assembleDebugAndroidTest` | PASS; gồm 2 test policy backup, 2 test pending-write UI và 1 test migration+budget mới. |
| `signingReport` / `apksigner verify` | Release `Config: none`; `apksigner` exit 1, `DOES NOT VERIFY` / missing signature metadata. Đây là bằng chứng APK chưa ký, không phải lỗi compile. |
| Static/APK review | 72 production Kotlin files; 45 XML parse được; 171 keys VI/EN đúng placeholders; dependency boundaries hợp lệ, schema export 1/2. Release DEX/APK không chứa Firebase/credentials/auth package hoặc service JSON đã bỏ. |
| Git hooks | Cú pháp shell PASS, fixture nhánh POSIX gọi đúng tasks và trả mã lỗi Gradle 17 khi giả lập failure. `core.hooksPath=.githooks` đang bật ở clone này; `git diff --check` PASS. |
| CI | Workflow/source và 3 tag `actions/checkout@v7`, `setup-java@v6`, `upload-artifact@v7` đã kiểm tra; không có remote Actions run trong audit. |
| API 26 | Chưa xác minh runtime. Image API 26 có sẵn; hai AVD QA riêng không boot thành công để chạy app (một mắc ở bước disk encryption, một không xuất hiện qua ADB). Đã dừng emulator QA; không wipe hoặc thay đổi emulator API 35 gốc. Không suy diễn kết quả API 35 thành pass API 26. |

Lần chạy tái hiện trước fix: Debug/Release build PASS, unit suite 97 tests với
96 pass và 1 failure ở regression pending save mới; các gate sau unit chưa chạy
trong lần đó. Kết quả này không được tính là một lần chạy tất cả gate thành công.

**Instrumentation API 35 ở audit ban đầu:** có kết quả PASS cho **63 ca riêng biệt**,
qua các lượt dưới đây. Audit ban đầu chưa có full suite pass liên tục; full run
PASS mới nằm trong mục Final Release Verification phía dưới:

| Lượt | Kết quả thực tế |
| --- | --- |
| Toàn bộ 63 ca, Wi-Fi/mobile data tắt | Host harness dừng sau 900s: **59 PASS, 1 failure PixelCopy**, 3 ca Wallet cuối chưa hoàn tất. Không tính lượt này là full suite PASS. |
| Ba ca Wallet chưa hoàn tất | **PASS: 3/3**, 82.162s. Gồm Wallet CRUD/select/move/guarded delete và 2 Wallet state/VI/EN/font/error cases. |
| Retry riêng ca System Theme | **PASS: 1/1**, 18.538s, cùng source/APK; không thêm suppression, bỏ assertion hoặc sửa app để retry. |

Đã parse status từng test và loại trùng theo `class#method`: 63/63 ca có ít nhất
một kết quả pass, 0 skip; **failure ban đầu vẫn được giữ trong báo cáo**. Bao gồm
4 migration/reopen cases, 2 packaged backup-policy cases, 2 pending-write UI cases,
CRUD/search/filter/date editor, Home/Budget/Statistics/Wallet và Settings/lifecycle/
VI/EN/Light/Dark/System. Không gọi toàn bộ lịch sử chạy này là “0 failures”.

Chạy test APK bằng `adb install -r -t`, gọi runner trực tiếp để tránh uninstall/
clear storage của app hiện hữu:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e timeout_msec 120000 com.ngyennhi.expensemanager.test/com.ngyennhi.expensemanager.HiltTestRunner
```

Lượt focused thêm `-e class` cho
`WalletFlowTest#createEditSelectMoveAndDeleteWalletWithoutChangingGlobalSpending,WalletScreenStateTest`
(đầy đủ package `com.ngyennhi.expensemanager.presentation`), và lượt retry dùng
`SettingsFlowTest#systemThemeTracksDeviceWhileExplicitChoicesStayFixed`. Các write
dùng Room in-memory hoặc DB/prefs fixture riêng; không ghi vào financial storage
production. Wi-Fi được trả về bật, mobile data giữ tắt như ban đầu.

Đã mở kiểm tra các ảnh fixture Statistics VI và Budget EN tạo từ lượt test hiện
tại; số tiền/nhãn, donut/legend, tháng và spent/remaining nhất quán với fixture.
Không dùng các ảnh này để tuyên bố đã hoàn tất review mọi màn hình/TalkBack.

Cold launch Debug sau cập nhật trả `Status: ok`, `LaunchState: COLD`, TotalTime
7,596 ms trong lúc Gradle và emulator QA khác cùng tải host; sau device tests,
cold launch production Debug với Wi-Fi tắt trả `Status: ok`, 3,660 ms. PSS sau idle
ở lần đo trước khoảng 81,751 KB. Đây chỉ là chẩn đoán trên emulator Debug, không phải benchmark release
hoặc bằng chứng đạt một SLA performance.

## Bảo toàn dữ liệu

Trước cài đặt: Room v2 có 4 giao dịch, 1 ví và 1 budget preference; integrity check
trả `ok`, foreign key check không có lỗi. Snapshot logic cục bộ gồm mọi field/ID,
wallet relation, sqlite_sequence, Room identity và budget/appearance values, lưu
trong `app/build/release-audit/` được Git ignore. Không đưa dữ liệu cá nhân vào Git.

Đã so sánh snapshot trước cài, sau `adb install -r`, sau device tests, sau retry và
sau production cold launch: **tất cả giống nhau**; 4 giao dịch, 1 ví, 1 hạn mức,
mọi field/ID/relations/sequence/Room identity và budget/appearance values được giữ
nguyên. SHA-256 logical snapshot: `fe32747251794b488de2f3d6e920772f38077108ca405439240072ff80dfeef4`.
Integrity `ok`, foreign key errors 0. Đây là cập nhật/cài lại **Debug code 2 → code 2**
với cùng chữ ký; không thay thế kiểm thử nâng cấp từ APK cũ bằng khóa release.

Evidence cục bộ (ignored): `verification.log`, `reproduction.log`, unit/lint reports,
`instrumentation-full.txt`, `instrumentation-focused.txt`,
`instrumentation-theme-retry.txt`, `device-results.json`, manifest/signature/DEX
inspection và các logical snapshots. Không publish artifact chứa dữ liệu cục bộ.

## Final Release Verification — lượt tiếp tục (2026-10-27)

Không sửa app/test source, dependency, schema hoặc cấu hình ký trong lượt này.
Source/schema fingerprints vẫn khớp `verified-source-inputs.json` của audit trước.
Không tạo keystore, không đọc/sửa mật khẩu ký, không commit/push hoặc tạo release.

| Check | Kết quả của lượt tiếp tục |
| --- | --- |
| Gradle gates | Chạy lại `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**, 1m 58s, 149 tasks (7 executed, 142 up-to-date). Unit task ở lượt gộp là `UP-TO-DATE`. |
| Unit execution mới | Chạy riêng `:app:testDebugUnitTest --rerun` với cùng cờ Gradle: **BUILD SUCCESSFUL**, 15s, 1 task executed. XML mới: **98 tests / 17 suites, 0 failure/error/skip**. |
| Lint / static / APK | 0 errors, 14 warnings, 1 hint; 171 keys VI/EN đúng placeholders, dependency boundaries và schema 1/2 hợp lệ. APK đúng `1.0.0`/code 2, minSdk 26, không INTERNET hoặc các auth package đã bỏ. |
| Unsigned release | 9,246,543 bytes; SHA-256 `d65fbf774b9594bd6df3e2dc0ed83511bc164db47b3de6bb89cef21d6342a23c`. `zipalign -c -P 16 4` exit 0; `apksigner verify` exit 1, `DOES NOT VERIFY / Missing META-INF/MANIFEST.MF`. Không cấu hình hay thực hiện ký. |
| API 35 — full attempt đầu | **52 PASS**, không có assertion failure được trả về; runner dừng ở ca 53/63, `TransactionFilterEditorTest#englishEditorSelectsInclusiveRangeAndAppliesAtLargeText`, vì `keyDispatchingTimedOut` (FocusEvent 60,009 ms). Ca 53 không có terminal test result; 10 ca tiếp chưa bắt đầu. ADB exit 0 nhưng không có `OK (...)`; harness ghi `passed=false`. Không tính là full suite PASS. |
| API 35 — focused retry | Sau khi AVD kết nối lại, chạy nguyên class `TransactionFilterEditorTest`: **PASS 2/2**, 22.948s, cùng source/APK/assertion. Không đủ bằng chứng xác định nguyên nhân ANR hay khẳng định lỗi đã được sửa. |
| API 35 — full attempt thứ hai (source trước chỉnh PixelCopy) | **PASS 63/63 trong một lượt liên tục**, 245.229s, 0 failure/error/skip; ADB exit 0, có `OK (63 tests)` và harness `passed=true`. AVD dùng software graphics, font scale 1.3 và app locale EN. Không gộp kết quả retry để tạo full pass và không xóa lịch sử các lượt trước. |
| API 35 — full attempt hiện tại (sau chỉnh PixelCopy) | **PASS 63/63 trong một lượt liên tục**, 527.54s, 0 failure/error/skip; ADB trả `OK (63 tests)`, gồm ca System Theme mới và ca Transaction Filter từng dừng ở lượt cũ. Snapshot Room/preferences sau run khớp SHA-256 `25d01cc3b8ac7857270f7665e93aa61d1fb2836f83546132a83292eecf6c070e` với snapshot trước đó. |
| API 26 startup/migration | Thử thêm AVD QA fixtures mới trong build dir với WHPX khả dụng. AVD 10 GB mặc định: transport ADB lên nhưng boot không hoàn tất; khi `-EncryptUserData` tắt, logcat báo `Cannot write real block device footer` / `Encrypted filesystem not validated`; AVD fresh 1 GB với mã hóa mặc định chuyển ADB offline trước boot. Không cài APK hoặc chạy instrumentation/migration API 26. Đã dừng riêng các QEMU fixtures; AVD khác và dữ liệu app API 35 được giữ nguyên. Runtime startup/migration gate vẫn mở. |

Lệnh Gradle gộp giống lượt trước nhưng bỏ `:app:signingReport`; unit execution mới:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun --console=plain --max-workers=1 --no-daemon --no-configuration-cache
```

API 35 mất kết nối sau full attempt đầu. Khi khởi động lại AVD `Pixel_5` với
`-no-snapshot` và software graphics, launcher báo exit `-1073741819`
(`0xC0000005`), nhưng máy ảo sau đó vẫn xuất hiện và boot xong qua ADB.
Không có ANR trace lấy được trước khi mất kết nối; sau boot,
`dumpsys activity lastanr` báo không có ANR từ lần boot này. Không quy kết
nguyên nhân cho app, GPU hay hypervisor khi chưa có trace.

Snapshot đầu lượt tiếp tục, sau ANR/AVD khởi động lại, trước full run thứ hai,
sau full run PASS và sau offline cold launch **giống nhau hoàn toàn**:
schema 2, 4 giao dịch, 1 ví, 1 budget preference; mọi field/ID/relation/sequence,
Room identity, budget và appearance được giữ nguyên. Integrity `ok`, FK errors 0;
SHA-256 logical snapshot `25d01cc3b8ac7857270f7665e93aa61d1fb2836f83546132a83292eecf6c070e`.
Baseline mới có appearance khác baseline audit cũ ngay trước thao tác đầu tiên;
các bảng Room và budget khớp audit cũ. Không ghi đè appearance để ép hash cũ.

Sau ANR, font scale là 1.3 và app locales là `[en]`. Lượt đầu không lưu hai giá trị
này trước run, nên không chứng nhận khôi phục về cấu hình ban đầu. Lượt full thứ
hai lưu riêng font/locale và snapshot trước run: **sau full run vẫn 1.3 / EN**.
Giữ hai giá trị theo baseline đã ghi nhận của full run thứ hai. File baseline PowerShell
là UTF-16; helper so sánh ban đầu đọc sai encoding và báo false mismatch.
Đã sửa cách decode và đối chiếu lại giá trị đã ghi, không đổi setting để ép pass;
giữ bản kết quả lỗi encoding riêng trong evidence.

Production Debug cold launch khi Wi-Fi/mobile data tắt: **Status: ok,
LaunchState: COLD, TotalTime 2,250 ms**. Không coi đây là benchmark release.
Sau đó Wi-Fi khôi phục bật, mobile data giữ tắt, giống trước full run. Không
uninstall, clear storage hoặc ghi giao dịch/budget/wallet production.

Evidence bổ sung nằm trong `app/build/release-audit/` (ignored):
`verification-continuation.log`, `unit-continuation-rerun.log`,
`continuation-artifacts.json`, `continuation-signature.txt`,
`continuation-zipalign.txt`, `instrumentation-continuation-full.*`,
`instrumentation-continuation-filter-retry.*`, `instrumentation-continuation-full-second.*`,
`continuation-final-device*.json`, `continuation-offline-cold-launch.txt`,
`continuation-api35-supervised*.log`,
`continuation-api26-emulator*.log`, `continuation-before/`,
`continuation-after-restart/`, `continuation-before-second-full/`,
`continuation-after-second-full/`, `continuation-after-offline-cold-launch/`.
Snapshot/log chứa dữ liệu cục bộ không được đưa vào Git hoặc publish.

## Đối chiếu các vấn đề còn mở (2026-10-27)

Lượt này kiểm tra lại artifact/source và bằng chứng hiện có, đọc trạng thái thiết
bị và kết quả CI từ xa. Không chạy mới Gradle/instrumentation, không sửa app/test
source, không ký hoặc cài/thay thế APK và không thay đổi font/locale/mạng.
Các script xác minh fingerprints, XML unit/lint, 63 test IDs và 5 snapshots của
lượt trước vẫn PASS; đây là kiểm tra lại bằng chứng, không phải lượt test mới.

| Mục | Kết quả đối chiếu / điều kiện đóng |
| --- | --- |
| **Signed Release APK — còn mở** | Người dùng đã báo ký bằng Android Studio. Tìm cả các thư mục bị Git ignore trong workspace vẫn chỉ thấy `app-release-unsigned.apk` cũ, SHA-256 `d65fbf77...`, `apksigner verify` exit 1; alignment PASS. Không suy ra APK ở nơi khác chưa ký. Cần đường dẫn tới đúng APK đã ký để xác minh chữ ký, certificate, `1.0.0`/code 2, manifest không debuggable, permissions và payload. |
| **Upgrade Release giữ dữ liệu — còn mở** | APK lấy chỉ đọc từ app đang cài trùng byte với Debug đã kiểm tra: SHA-256 `e2cb46df...`, `application-debuggable`, v2 signature PASS, certificate SHA-256 `1f244570f4ed83462710d8400d8d88d8a4711f0a2767d2336535838374a55103`. Không phải bằng chứng của bản ký release. Cần APK cũ thực tế và APK release mới có chữ ký cập nhật tương thích, snapshot trước/sau `install -r` và cold launch. Không uninstall/clear hoặc ký lại để ép cài đè. |
| **API 26 startup/migration — còn mở** | ADB cuối cùng chỉ có `emulator-5554`, API 35. Các thử mới với WHPX, encryption on/off và data partition 1 GB không đạt `sys.boot_completed`; một logcat cho thấy lỗi crypt footer. `WalletMigrationTest` chạy trong suite API 35, nên không chứng nhận Room/startup runtime trên API 26. Workflow mới nhắm API 26 emulator cho `OfflineFlowTest` (app startup/navigation với storage cô lập) và `WalletMigrationTest`; chưa có remote result. Gate vẫn mở đến khi job thực sự chạy xanh, đồng thời chưa thay thế signed in-place upgrade trên thiết bị. |
| **CI — pass một phần, gate mở đến remote run** | Đã đọc [run 40480894011](https://github.com/nhingyen/personal-expense-manager/actions/runs/40480894011), `success`, created `2026-10-27T09:27:17Z`, đúng HEAD `90d39062fa26f2e8a0839d90800d46b0ad459c09`; run đó dùng workflow cũ chỉ có `assembleDebug`, `testDebugUnitTest`, `lintDebug`. Workflow hiện trong working tree thêm `assembleRelease` unsigned vào build/unit/lint job và một API 26 emulator job build Release + chạy `connectedDebugAndroidTest` cho startup/migration `OfflineFlowTest`/`WalletMigrationTest`. Chưa thể chứng nhận cấu hình mới đến khi remote CI chạy trên đúng source; không commit/push để kích hoạt. CI không ký APK và không cần secret ký. |
| **ANR / PixelCopy — theo dõi** | PixelCopy trong ca System Theme đã được tránh bằng cách không chụp ảnh khi đổi global night mode; test thay vào đó đợi system-bar appearance được áp dụng. Focused test PASS 4/4 và full suite trên source hiện tại PASS 63/63 liên tục trong 527.54 giây. Ca Transaction Filter từng dừng trước đó cũng PASS trong lượt mới. Nguyên nhân ANR trước đây chưa xác định; trace lần trước boot không còn, nên tiếp tục theo dõi khi chạy suite sau này. |
| **Accessibility / cấu hình thiết bị — còn mở** | Baseline API 35 ghi nhận font scale 1.3, TalkBack tắt (`enabled_accessibility_services=null`, `accessibility_enabled=0`), locale EN; sau lần thử đã khôi phục và xác nhận đủ ba giá trị. TalkBack có package trên emulator nhưng chưa review được traversal/announcement của app: khi thử bật service, app mở trên màn hình quyền hệ thống và UIAutomator trả về cây của một ứng dụng khác, không phải Personal Expense Manager. Thử nghiệm dừng tại đó; không coi đây là PASS. `SettingsScreenTest` chỉ kiểm tra riêng Settings với density 2.0; các ca text lớn khác không chứng nhận mọi màn hình ở font/display lớn nhất, landscape, IME/date picker và gesture/3-button navigation. Font scale 2.0 chưa được xác minh trên app. Cần checklist thực tế với baseline/khôi phục cấu hình được ghi nhận. |
| **Process death / lỗi storage thực tế — còn mở** | Transaction/Wallet draft đang ở ViewModel, không có `SavedStateHandle`; giới hạn mất draft sau process death đã công bố. `ActivityScenario.recreate`, fake failure và cold launch không chứng nhận system-initiated process death lúc đang edit/sheet/route phụ hoặc disk-full/SQLite/prefs lỗi thực tế. Cần test phục hồi dữ liệu đã lưu và hành vi đúng giới hạn đã công bố trên fixture riêng; không tự mở rộng scope thành tính năng lưu mọi draft. |
| **Performance / 16 KB runtime — chưa xác minh** | Release APK unsigned hiện có 9,246,543 bytes; Gradle release `optimization.enable=false`. Code scan xác nhận các collector Home/Transactions/Statistics/Wallet map/lọc/tính summary, balance và thống kê trong `viewModelScope` (main dispatcher mặc định); list UI dùng LazyColumn và stable keys ở các màn hình chính. Đây là vùng cần đo khi dữ liệu lớn, chưa phải lỗi hiệu năng đã tái hiện. Chẩn đoán cũ trên emulator Debug: cold launch 3,660 ms khi yên hơn (một lần 7,596 ms lúc host/emulator bận), PSS khoảng 81,751 KB sau idle; không có nhiều lượt/percentile, frame timing hoặc release build, nên không phải benchmark/SLA. Thiết bị hiện có `PAGE_SIZE=4096`. Alignment APK/native 16 KB đã kiểm tra ở audit trước, chưa có runtime trên thiết bị page-size 16 KB. Không bật R8 hay refactor chỉ từ giả định hiệu năng; cần benchmark release lặp lại với dataset/tiêu chí cố định, jank/frame timing và memory trước khi đặt ngân sách hoặc đổi code. |
| **Backup/transfer OEM — giới hạn bằng chứng** | Manifest/rules và packaged policy tests đã PASS. Chưa có thử backup/transfer thực tế trên OEM; giữ nguyên giới hạn này, không gọi là bug backup mới và không suy ra tương thích mọi OEM. |

Android Studio cho phép ký từng APK bằng wizard và chọn thư mục output; cấu hình
Gradle ký tự động là một luồng riêng. Vì vậy, điều kiện đóng gate là **kiểm tra
đúng Signed APK và upgrade**, không phải bắt buộc thêm signing config/secret vào
repo. Android kiểm tra certificate khi cập nhật. Nguồn: [Android Developers —
Sign your app](https://developer.android.com/studio/publish/app-signing).

Bằng chứng mới (ignored, không publish): `open-issues-signature.txt`,
`open-issues-remote-ci.json`, `open-issues-installed-base.apk`,
`open-issues-installed-signature.txt`, `open-issues-device.json`,
`open-issues-lastanr.txt`. APK lấy từ thiết bị chỉ chứa code/resources; không lấy
database/preferences hoặc truy cập keystore/mật khẩu trong lượt này.

## Manual release gates

- Signed APK người dùng tạo đã được xác minh ở cập nhật 2026-10-28: chữ ký v2,
  certificate và manifest/version PASS. Còn phải thử cập nhật trên thiết bị có bản
  cũ để xác minh certificate tương thích và dữ liệu được giữ nguyên.
- Upgrade từ APK cũ thực tế (đặc biệt schema v1/auth-era) sang APK release đã ký,
  trên API 26 và thiết bị Android mới; giữ cả budget/appearance/platform locale.
- TalkBack đọc trọn luồng, font/display size lớn nhất, bàn phím/ngày picker,
  landscape, gesture/3-button navigation, VI/EN và Light/Dark/System trên máy thật.
- System-initiated process death khi đang edit/sheet/route phụ; disk-full và lỗi
  SharedPreferences/SQLite thực tế. Các fake failure tests không thay thế các ca này.
- Benchmark dữ liệu lớn, cold start/jank/memory của bản release trên thiết bị mục tiêu;
  chạy CI trên remote trước merge/phát hành. Full instrumentation mới nhất PASS 63/63
  trên API 35; chưa chứng nhận API 26. Vẫn theo dõi ANR/PixelCopy lịch sử.

Không xóa app data, không uninstall/wipe emulator, không destructive migration,
không commit/push, không tạo tag/GitHub Release hay phát hành APK.

## Build, Tests, Lint — verification mới nhất (2026-10-27)

Không đổi app/test source trong lượt kiểm tra này. Build, unit tests và lint chạy
trên working tree hiện tại:

| Check | Kết quả |
| --- | --- |
| `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` | **BUILD SUCCESSFUL**, 2m17s, 149 tasks (7 executed, 142 up-to-date). Unit task trong lượt gộp là up-to-date, nên không dùng lượt này làm bằng chứng fresh unit execution. |
| Unit tests fresh | `:app:testDebugUnitTest :app:lintDebug --rerun-tasks`: **BUILD SUCCESSFUL**, 1m51s, 43 tasks executed. XML: **98 tests / 17 suites, 0 failures, 0 errors, 0 skipped**. |
| Lint fresh | Cùng lượt rerun: **0 errors, 14 warnings, 1 hint**; report `app/build/reports/lint-results-debug.txt`. |
| API 35 instrumentation, lần Gradle | `connectedDebugAndroidTest` bắt đầu trên Pixel_5/API 35. Lượt bị ngắt ở mức công cụ sau khi 56 logcat test được tạo; không có XML hoặc terminal summary. **Incomplete, không tính PASS/FAIL.** |
| API 35 instrumentation, lượt trực tiếp | Runner báo `numtests=63`; 42 test có status PASS. Test 43, `SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt`, không trả status cuối; guest ADB ngừng phản hồi. Không có assertion failure status trước khi dừng, nhưng cũng không có `OK (63 tests)`. Lượt **incomplete**, không tính full-suite PASS. |
| Emulator/configuration | Đã dừng riêng Pixel_5 emulator bằng console (không wipe/uninstall/clear app data) sau khi guest shell ngừng phản hồi. Hai lần cold-start sau đó không giữ được emulator online; `adb devices` cuối cùng không liệt kê thiết bị. Không xác minh lại font/locale/TalkBack sau test locale bị ngắt. Các AVD khác không bị sửa. |
| Source whitespace | `git diff --check` PASS. |

Full suite **63/63 PASS trong 527.54s** ở lượt trước vẫn là bằng chứng hợp lệ cho
lần chạy đó; lượt mới không thể tái-xác nhận vì bị kẹt ở test platform locale.
Không gộp hai lượt incomplete với kết quả lịch sử để tuyên bố một pass mới.

**Kết luận hiện tại: Not Ready for distribution.** Debug/Release build, 98 unit
tests, lint và xác minh chữ ký Signed Release APK thành công. Tuy nhiên lượt
instrumentation mới không hoàn tất; API 26 startup/migration, signed upgrade và
remote run của CI mới chưa xác minh. Build/test/lint và kiểm tra chữ ký không đóng
các gate runtime/upgrade này. Không tạo keystore, không thay đổi mật khẩu ký,
không commit/push, không tạo release.

## Root-cause follow-up — 2026-10-28

| Mục | Bằng chứng mới | Trạng thái |
| --- | --- | --- |
| Test đổi locale | `SettingsFlowTest` gọi chọn VI, đặt framework per-app locale sang EN, chờ state/resource cập nhật, rồi chọn System. `MainActivity.onResume` gọi `refreshLanguage()`; repository đọc AppCompat application locales. Logcat của lượt Gradle ghi test start 23:11:07, runner `finished` 23:11:22, ca tiếp theo start 23:11:22. Không có `ANR in`, `FATAL EXCEPTION` hoặc exception app trong log đã lưu. | Không chứng minh bug app. Không có terminal JUnit XML nên logcat `finished` không đủ để tuyên bố PASS. Lượt direct run sau đó kẹt tại cùng ca đồng thời ADB guest ngừng phản hồi; do lượt trước bị ngắt ở host, không loại trừ runner overlap. Nguyên nhân còn mở. |
| Emulator/UI | Log cùng test ghi TalkBack permission activity và Android permission controller nổi trên app, nhiều lần 30–110 skipped frames, SurfaceSyncGroup 1s timeout, system_server contention. Khi tái khởi động, Pixel_5 QEMU thoát trước khi ADB online; AVD API 26 read-only lên ADB rồi offline trước `sys.boot_completed`. | Bằng chứng mạnh hơn cho scheduling/emulator/harness instability so với assertion/app exception, nhưng không có ANR trace/host crash code đủ kết luận dứt khoát. Không wipe AVD, không xóa dữ liệu. |
| Focused test trước full | Không chạy được lượt riêng trong lần tiếp tục này: Pixel_5 không boot ổn định; các log lịch sử không thay thế chạy focused mới. | **INCOMPLETE**; không tăng timeout hoặc sửa/bỏ assertion. Full suite chưa được chạy lại sau một focused PASS. |
| Signed APK (trạng thái tại thời điểm 2026-10-28, trước cập nhật bên dưới) | Khi đó tìm thấy unsigned Release và chưa tìm thấy APK người dùng báo đã ký; unsigned artifact SHA-256 `d65fbf774b9594bd6df3e2dc0ed83511bc164db47b3de6bb89cef21d6342a23c`. | Đã được thay thế bởi kết quả xác minh signed artifact ghi ở phần cập nhật 2026-10-28 bên dưới. Signed upgrade vẫn chưa chạy. |
| API 26 | AVD `ExpenseManager_QA_API26_Plain` chạy read-only; ADB thấy emulator tạm thời ở API 26, nhưng nó offline trước `sys.boot_completed`. Không cài app/test APK. | Startup/migration **INCOMPLETE**; Room migration API 26 chưa chạy. |
| CI mới | `.github/workflows/android-ci.yml` local có build Debug/unsigned Release/unit/lint và job `reactivecircus/android-emulator-runner@v2` API 26 chạy `assembleRelease` + `connectedDebugAndroidTest` lọc `WalletMigrationTest,OfflineFlowTest`. `gh run list` ngày 2026-10-28 vẫn trả run mới nhất `40480894011` (2026-10-27 09:27Z, HEAD `90d39062...`), workflow cũ. | Chưa có run thực tế trên workflow/source mới; build unsigned không kiểm tra ký và instrumentation API 26 chưa có report. `actionlint` không cài sẵn, chưa có workflow linter run. |
| Build/Unit/Lint | Lượt 2026-10-27: Gradle build Debug/Release/test APK PASS; rerun fresh unit **98/98 PASS**; lint **0 errors, 14 warnings, 1 hint**. | PASS theo lượt ghi ngày 2026-10-27; không chạy lại trong điều tra root cause này. |

Không xác nhận được root cause để sửa an toàn; không thay đổi app/test source. Kết luận
vẫn **Not Ready**: signed upgrade, API 26 startup/migration và remote CI run còn
thiếu bằng chứng. Focused locale test và full instrumentation đã được chạy lại,
với kết quả ghi ở phần tiếp theo. Xác minh chữ ký APK được bổ sung bên dưới.

## Signed Release APK verification — 2026-10-28

Người dùng chỉ vị trí artifact bằng yêu cầu xác minh; tìm thấy
`app/release/app-release.apk`. `app/build/outputs/apk/release/app-release.apk`
có cùng kích thước và SHA-256, nên hai đường dẫn trỏ tới cùng nội dung APK.
`apksigner verify --verbose --print-certs` trả thành công: v2 **true**, một signer,
v1/v3/v3.1/v4 **false**. Certificate DN là
`C=VN, ST=Da Nang, L=Da Nang, O=Personal Projects, OU=Mobile Development,
CN=Nhi Nguyen`; certificate SHA-256
`bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.
Artifact SHA-256 là
`AF23C7B1998250EDAB65DB233650AD495DF05666C3F8B41C6144865DD9474E69`.
`aapt dump badging` xác nhận package `com.ngyennhi.expensemanager`, version
`1.0.0` / code `2`, minSdk `26`, targetSdk `37`. **Release signing verification:
PASS** cho đúng artifact này. Chữ ký không phải Debug certificate. Không đọc
keystore/mật khẩu. Chưa cài APK lên bản cũ; certificate continuity/in-place signed
upgrade và giữ dữ liệu vẫn **INCOMPLETE**. API 26 startup/migration và CI mới
vẫn chưa xác minh ở thời điểm ghi mục này;
kết quả instrumentation mới hơn được ghi ở phần tiếp theo. Kết luận tổng thể vẫn
**Not Ready**.
Certificate so sánh với APK Debug trong cùng workspace có DN
`C=US, O=Android, CN=Android Debug` và fingerprint SHA-256
`1f244570f4ed83462710d8400d8d88d8a4711f0a2767d2336535838374a55103`, khác với
certificate của Signed Release APK xác minh ở trên.

## Final Release Verification continuation — 2026-10-28

### Signed upgrade — INCOMPLETE

Workspace APK inventory không có signed artifact versionCode > 2. `app-release.apk`
đã xác minh là versionCode 2; các APK versionCode 1 tìm thấy đều ký bằng
`C=US, O=Android, CN=Android Debug`, không khớp certificate Release.
Do đó không thể kiểm tra cập nhật giữ nguyên package/certificate và tăng versionCode
bằng artifact hiện có. Không thay versionCode hoặc tạo/ký APK thử nghiệm; việc đó
cần signing key và không được thực hiện trong giới hạn không truy cập keystore/
mật khẩu. Signed upgrade và Room/SharedPreferences retention **INCOMPLETE**.

Kế hoạch khi có APK kế tiếp cùng certificate: ghi snapshot Room (database identity,
wallets, transactions, foreign keys, `sqlite_sequence`) và SharedPreferences
(budget/theme) cùng platform locale trên bản cũ; `adb install -r` APK versionCode
tăng trên đúng AVD mà không uninstall; xác minh `apksigner` certificate, package
version và snapshot sau cài đặt/cold start. Artifact versionCode > 2 chưa có nên
các bước này chưa chạy.

### Locale focused test và full instrumentation — PASS API 35

Lệnh focused đầu tiên bị PowerShell tách đối số `#method`, Gradle không tìm thấy
task tương ứng; không có test nào chạy trong lần gọi đó. Chạy lại với runner
argument được quote:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ngyennhi.expensemanager.presentation.SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt' --console=plain --max-workers=1 --no-daemon --no-configuration-cache
```

Focused result XML: **1 test, 0 failures/errors/skips**, test time 5.031s.
Assertion và `waitUntil(10_000)` giữ nguyên. Sau focused PASS, chạy đúng một full
runner trên `emulator-5554` (Pixel_5/API 35), với `ANDROID_SERIAL=emulator-5554`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --max-workers=1 --no-daemon --no-configuration-cache
```

Full result XML timestamp 2026-10-28 04:24Z: **63 tests / 21 suites, 0 failures,
0 errors, 0 skipped**, runner exit code 0; Gradle BUILD SUCCESSFUL in 4m01s.
Locale test có trong suite. Không có test runner thứ hai chạy đồng thời. Kết quả
này PASS cho API 35, không suy diễn thành API 26.

### Side effect trên emulator — cần lưu ý

Trước focused test, `pm path` xác nhận package `com.ngyennhi.expensemanager`
đã được cài trên Pixel_5. Sau khi Gradle test kết thúc, `pm list packages` không
còn package này; việc gỡ app không được yêu cầu. Hilt test dùng Room in-memory và
`task010_test` preferences, nhưng không chụp snapshot persistent app data trước
lượt chạy. Vì thế không thể xác nhận có hay không dữ liệu persistent bị ảnh hưởng.
Sau đó full suite chạy trên cùng Pixel_5 khi package đã vắng mặt; Gradle runner
đã cài app cho instrumentation và package lại vắng sau lượt chạy. Không có lệnh
wipe/clear data hay cài/gỡ thủ công nào được gọi. Đây không phải bằng chứng
Room/SharedPreferences được giữ qua signed upgrade.

### API 26 startup/migration — INCOMPLETE

Khởi động AVD riêng `ExpenseManager_QA_API26_Plain` không dùng `-wipe-data`.
ADB liệt kê emulator nhưng guest shell `getprop`/`pm` không trả kết quả và
`sys.boot_completed` không xác nhận. Không cài APK, không chạy `OfflineFlowTest`
hoặc `WalletMigrationTest` trên API 26. Không xóa/wipe dữ liệu AVD.

### CI workflow — local review PASS; remote execution INCOMPLETE

`.github/workflows/android-ci.yml` có trigger `pull_request` vào `main`/`develop`
và push vào hai nhánh này. Workflow có job build Debug/unsigned Release/unit/lint
và job API 26 chạy `assembleRelease`, `OfflineFlowTest`, `WalletMigrationTest`;
reports upload với `if: always()`. PR chưa được mở từ working tree này và không có
run trên workflow mới. Read-only `gh run list` vẫn cho kết quả mới nhất là run
`40480894011`, success ngày 2026-10-27 trên HEAD cũ `90d39062...`; không xác minh
các thay đổi hiện tại. Sau khi PR được mở, cần xác nhận cả hai job của đúng commit
PR hoàn tất thành công, xem test reports/API 26 boot và kiểm tra branch protection
đòi các check này nếu cần merge gate. Không commit/push trong lượt này.

**Kết luận sau continuation: Not Ready.** Locale focused test và API 35 full suite
PASS. Signed upgrade/data retention, API 26 startup/migration và remote CI mới vẫn
INCOMPLETE. Không thay đổi app/test code.

## Launcher Icon verification — 2026-10-28 (TASK-015)

[TASK-015](../tasks/TASK-015.md) xác nhận logo Image Asset Studio: đủ mười Legacy
Icon, năm foreground và hai adaptive XML; manifest dùng đúng icon/roundIcon.
Build Debug và unsigned Release mới **PASS** (`BUILD SUCCESSFUL in 1m 42s`);
15/15 WebP nguồn khớp nội dung trong từng APK. Cài Debug bằng `adb install -r`
trên Pixel_5 / API 35 khi package chưa tồn tại và quan sát logo ví tím đúng trong
App Drawer. Ảnh/log giữ cục bộ trong `app/build/launcher-icon-audit/`, ngoài Git.
Home Screen, themed icon và launcher khác còn cần kiểm tra thủ công.

Không sửa/xóa resource hợp lệ, manifest, Room, versionCode hoặc signing; không
uninstall/clear data, không đọc keystore/mật khẩu. Thêm `/app/release/` vào
`.gitignore` để giữ output Android Studio ngoài Git. Cần tạo lại Signed Release
APK từ source/resources hiện tại và xác minh đúng artifact đó. Kết quả chữ ký
APK cũ ở trên không áp dụng cho APK vừa build. Signed upgrade/data retention,
API 26 và remote CI vẫn còn mở; kết luận tổng thể vẫn **Not Ready**.

## Signed Upgrade v2 → v3 — 2026-10-28

Người dùng cho phép tăng versionCode sau khi bảo toàn APK v2; phần này cập nhật
scope trước đó chỉ kiểm tra artifact. Quy trình chi tiết và hướng dẫn ký trong
Android Studio: [SIGNED_UPGRADE_v2_v3.md](SIGNED_UPGRADE_v2_v3.md).

### PASS — artifact và build

- Xác minh lại `app/release/app-release.apk` bằng `apksigner verify --verbose
  --print-certs`: verify thành công, một signer, signature scheme v2=true.
  `aapt dump badging` xác nhận package `com.ngyennhi.expensemanager`, code 2,
  name 1.0.0, minSdk 26/targetSdk 37.
- Certificate SHA-256:
  `bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`;
  APK SHA-256:
  `AF23C7B1998250EDAB65DB233650AD495DF05666C3F8B41C6144865DD9474E69`.
- **Trước khi đổi code/build**, copy sang
  `app/release/upgrade-verification/v2/app-v1.0.0-vc2-release.apk`, verify lại
  chữ ký/hash và đặt chỉ đọc. Sau build v3, hash vẫn khớp. Archive không ở trong
  `app/build`, được Git ignore; không ghi đè v2.
- Đổi đúng một dòng `versionCode = 2` thành `3` trong `app/build.gradle.kts`;
  giữ versionName 1.0.0 và signing. Build
  `.\gradlew.bat :app:assembleRelease --console=plain --max-workers=1`:
  **BUILD SUCCESSFUL trong 26s**, 54 tasks, 12 executed/42 up-to-date;
  `lintVitalRelease` đi kèm PASS. Không chạy lại Debug, unit/full lint hoặc
  instrumentation trong continuation này.
- APK mới **chưa ký**:
  `app/build/outputs/apk/release/app-release-unsigned.apk`, SHA-256
  `46381c677cd98b7134d089a60ff0314e12755976356df288b9a17edd6f5ee309`.
  Giữ thêm bản sao ở `app/release/upgrade-verification/v3-unsigned/`.
  Metadata đúng code 3/name 1.0.0; đủ 15 WebP logo khớp source. Hash của cả
  138 file trong `app/src/main` và `app/schemas` không đổi trong lượt này.

### PASS — baseline v2 trên emulator riêng

Tạo AVD `ExpenseManager_SignedUpgrade_API35`, Google APIs x86_64/API 35, serial
`emulator-5558`. Boot đầu thoát khi chưa hoàn tất; retry `-gpu host -feature
-Vulkan` lên boot_completed=1. Không wipe AVD hoặc tác động dữ liệu hai emulator
cũ. Xác nhận package chưa có trên AVD mới, cài **APK v2 đã archive** bằng
`adb install -r`: Success; cold launch Status ok. Không dùng APK Debug để thay
thế. Root chỉ ADB trên AVD Google APIs riêng nhằm sao chép dữ liệu Release.

Tạo mẫu bằng UI, không chèn SQL/sửa XML:

| Mẫu đã lưu | ID / liên kết | Giá trị |
| --- | --- | --- |
| Default wallet | wallet 1 | Opening/balance 0 |
| UpgradeCash, CASH | wallet 2 | Opening 1.000.000; balance 1.375.000 |
| UpgradeBank, BANK | wallet 3 | Opening 2.000.000; balance 1.800.000 |
| UpgradeIncome, Salary, INCOME | transaction 1 → wallet 2 | 500.000, ngày 2026-10-28 |
| UpgradeFood, Food, EXPENSE | transaction 2 → wallet 2 | 125.000, ngày 2026-10-28 |
| UpgradeTravel, Travel, EXPENSE | transaction 3 → wallet 3 | 200.000, ngày 2026-10-28 |
| Budget | monthly_budget:2026-10 | 1.000.000; spent 325.000; remaining 675.000 |
| Settings | appearance/theme, platform locale | LIGHT, en |

UI xác nhận tổng ví **3.175.000**, thu **500.000**, chi **325.000**, net
**175.000**. Home hiển thị Used 32% (325.000/1.000.000 là 32,5%).
Snapshot `app/release/upgrade-verification/before-v2/snapshot.json` ghi UID
**10209**, Room user_version **2**, identity
`ffda5e095f46485f8c26d51bae071165`, `sqlite_sequence` wallets/transactions
**3/3**, `foreign_key_check` rỗng, `integrity_check=ok`. Snapshot JSON SHA-256:
`ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f`.
DB/WAL/SHM, preferences, metadata và ảnh UI giữ cục bộ dưới cùng thư mục
`app/release/upgrade-verification/`, ngoài Git. App v2 hiện force-stop sau capture;
dữ liệu vẫn trên AVD, không tạo lại fixture trước upgrade.

Đã mở launcher khi app stopped và xem ảnh
`emulator/v2-app-drawer.png`: APK v2 archive vẫn dùng icon Android mẫu cũ.
Đó là baseline hợp lệ trước cập nhật; APK v3 unsigned chứa logo ví tím mới.
Chưa xác minh việc launcher đổi từ icon cũ sang mới sau signed upgrade.

### Công cụ đối chiếu và giới hạn

Thêm `scripts/signed_upgrade_snapshot.py` và host checks. Công cụ từ chối AVD
khác tên, yêu cầu boot/root và version/hash APK đúng; force-stop rồi copy private
data, chỉ đọc SQLite trên bản sao máy tính. Compare toàn bộ rows/IDs/wallet_id,
schema/identity, sequence, FK/integrity, số dư, nội dung prefs, locale và UID.
Lưu cả file thô để điều tra; không dùng byte hash DB làm điều kiện duy nhất.

Lượt synthetic đầu lỗi cleanup Windows vì connection SQLite chưa đóng; sửa bằng
explicit closing và chạy lại PASS. Capture đầu từ chối vì Android 15 dùng
`appId` thay `userId`; bổ sung parser cả hai dạng, kiểm tra UID chủ storage và
host regression rồi capture thành công. Lệnh cuối
`python -B -m unittest discover -s scripts -p test_signed_upgrade_snapshot.py -v`
**7/7 PASS**; đây là dữ liệu tổng hợp trên máy tính, không phải signed upgrade.
Không chạy instrumentation trên AVD giữ dữ liệu.

### INCOMPLETE — chờ Signed v3 và kiểm tra sau upgrade

Chưa có APK v3 đã ký được cung cấp/xác minh. Người dùng ký bằng Android Studio
với cùng Release key, lưu riêng trong
`app/release/upgrade-verification/v3-signed`, rồi cung cấp đường dẫn APK. Không
truy cập keystore/mật khẩu hoặc thêm signing config. Chưa chạy install v3,
compare sau install/cold launch, kiểm tra UI sau upgrade hoặc logo trên v3 đã ký.
Logo Debug API 35 từ TASK-015 không thay thế bằng chứng logo sau signed upgrade.

**Kết luận: Not Ready.** v2 artifact/archive, v3 unsigned build, baseline v2 và
host tooling PASS. Signed v3 verification, v2 → v3 data retention/icon, API 26
startup/migration và remote CI còn INCOMPLETE. Không xóa dữ liệu, uninstall,
thay Room schema, commit/push hoặc tạo GitHub Release.

## Signed Upgrade execution — PASS API 35, 2026-10-28

Đã nhận APK v3 tại
`app/release/upgrade-verification/v3-signed/release/app-release.apk`. Xác minh
lại bằng `apksigner verify --verbose --print-certs` và `aapt dump badging`:

- Package `com.ngyennhi.expensemanager`, versionCode **3**, versionName
  **1.0.0**, minSdk 26/targetSdk 37.
- Một signer, signature scheme v2 hợp lệ; certificate SHA-256 trùng v2:
  `bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.
- Signed v3 SHA-256:
  `1DD171FB8BE0118A37B5C611B4162BBBECD608891B96E54FFBFB659B0E8BAD79`.
  Archive v2 vẫn giữ nguyên SHA-256 đã ghi ở trên. Đối chiếu ZIP v3 xác nhận
  **15/15 launcher WebP khớp source**.

### Kiểm tra điều kiện trước cài — PASS

`emulator-5558` vẫn là `ExpenseManager_SignedUpgrade_API35`, boot hoàn tất;
package đang cài code 2, hash base APK khớp v2 đã archive, UID/chủ storage
**10209**. Chụp fresh snapshot `preflight-v2`; toàn bộ metadata/state/hash file
thô đều khớp baseline trước đó. SHA-256 của JSON vẫn là
`ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f`.
Điều kiện chỉ cài khi thiết bị/dữ liệu khớp đã được đáp ứng; không tạo lại mẫu.

### Cập nhật và kiểm tra bảo toàn — PASS

Đã chạy đúng lệnh:

```powershell
adb -s emulator-5558 install -r app/release/upgrade-verification/v3-signed/release/app-release.apk
```

Kết quả **Success**, exit 0. Package code **3**, name 1.0.0, UID vẫn **10209**;
CE/DE data inode trong package report cũng giữ nguyên. Capture và `compare`
với `before-v2` đều PASS ở hai mốc **ngay sau cài** và **sau cold launch/UI**.

| Dữ liệu | Trước v2 → sau v3 |
| --- | --- |
| Wallet IDs và số dư | IDs 1/2/3; Default 0, UpgradeCash 1.375.000, UpgradeBank 1.800.000; tổng 3.175.000 |
| Transactions | Cả ba bản ghi, IDs 1/2/3 và toàn bộ type/amount/category/date/note giữ nguyên; wallet_id 2/2/3 |
| Thu/chi/net tháng 10 | 500.000 / 325.000 / 175.000, UI Home khớp |
| Room | user_version 2, identity `ffda5e095f46485f8c26d51bae071165`, schema/foreign keys nguyên vẹn |
| Sequence và integrity | wallets/transactions 3/3; foreign_key_check rỗng; integrity_check ok |
| Ngân sách | `monthly_budget:2026-10` giữ 1.000.000; spent 325.000, remaining 675.000 |
| Preferences/locale | Toàn bộ XML theo nội dung khớp; theme LIGHT, app locale en; UI chọn English/Light |

Cold launch bằng `am start -W` trả **Status ok / LaunchState COLD**. Đã xem Home,
Budget, Wallets, danh sách Transactions và từng detail của UpgradeIncome,
UpgradeFood, UpgradeTravel; tên ví/số tiền/note khớp dữ liệu gốc. Đã quan sát
**logo ví tím mới trên Home Screen và App Drawer**, thay icon Android mẫu của
v2; nhấn icon mở được ứng dụng. Không clear launcher data hoặc reboot.

### Evidence và giới hạn

Evidence cục bộ/Git ignore:
`app/release/upgrade-verification/run-v2-v3-20261028/`:

- `artifact-verification.json`, `preflight-comparison.json`, `install-v3.txt`,
  `package-after-install.txt`, `result.json`.
- `preflight-v2`, `after-v3-install`, `after-v3-cold-launch`: raw tar, DB/WAL/SHM,
  preferences, package metadata và snapshot JSON.
- `ui/`: XML/PNG Home, Budget, Settings, Wallets, Transactions, ba detail,
  Home Screen/App Drawer, mở app từ icon; `cold-launch.txt`, `checks.json`.

Snapshot SHA-256 sau install:
`8364d8529c43c2f0a766e0a3b6ca32b977ac4e4b916133c8fa49afb1bd9286e0`;
sau cold launch:
`9864a955c56e02af4172a4993fd3850368eae66e093a0c7cf46d10830f2520e4`.
JSON khác baseline vì metadata code/hash APK đã đổi. Sau install, private file
hashes còn khớp baseline; sau cold launch chỉ WAL/SHM khác hash. Toàn bộ dữ liệu
Room theo nội dung, main DB và preferences vẫn khớp; không coi WAL/SHM đổi byte
là mất dữ liệu.

**Signed Upgrade v2 → v3: PASS trên API 35 với fixture này. Không có FAIL trong
lượt thực thi.** Không chạy instrumentation hoặc Gradle build/test/lint; không
thay code/schema/version/signing, không uninstall/wipe/clear data, không đọc
keystore/mật khẩu, không commit/push hoặc tạo Release. AVD giữ v3 cùng dữ liệu
mẫu, app stopped sau capture cuối. Không downgrade về v2.

**Release tổng thể vẫn Not Ready**: API 26 startup/migration, remote CI và manual
gates còn mở. Signed v3 verification và signed-upgrade/data-retention/icon gate
được đóng cho API 35; không tiếp tục liệt kê chúng là thiếu bằng chứng ở mốc này.

## CI emulator profile — Run 40648694159, 2026-10-28

Đã đọc metadata và failed log bằng GitHub CLI, không kích hoạt rerun:
[run 40648694159](https://github.com/nhingyen/personal-expense-manager/actions/runs/40648694159),
HEAD `ebd8ffb0cfd5ba9168f46929a193af5af3ebacc1`, conclusion **failure**;
job [`api26-release-migration`](https://github.com/nhingyen/personal-expense-manager/actions/runs/40648694159/job/122683442945)
**failure**. Log ghi `profile: Pixel 2`, `Hardware profile: Pixel 2`, rồi lúc
07:14:44Z: `Error: No device found matching --device Pixel 2.`

Nguyên nhân: override hardware profile không tồn tại trong danh sách device của
avdmanager trên runner. Job lỗi ở cấu hình AVD, chưa chứng nhận startup/migration
trên API 26; không phải bằng chứng test assertion của app thất bại.

Working tree đã có bản sửa bỏ `profile: Pixel 2` trước lượt này; giữ nguyên bản
sửa để emulator-runner dùng profile mặc định. So với HEAD, YAML chỉ xóa đúng
một dòng đó. Giữ API 26, image `google_apis`/`x86_64`, emulator options, lệnh
`:app:assembleRelease :app:connectedDebugAndroidTest` và hai lớp
`WalletMigrationTest`, `OfflineFlowTest`. Không bỏ test, không thêm
`continue-on-error`, không sửa app code hoặc dependency.

| Kiểm tra | Trạng thái |
| --- | --- |
| YAML parse/không trùng key, triggers và jobs | PASS — PyYAML BaseLoader với kiểm tra duplicate keys |
| Diff so với HEAD chỉ bỏ profile; API/image/arch/tests giữ nguyên | PASS — đối chiếu cấu trúc YAML và assertions |
| `git diff --check` | PASS |
| Run lỗi đã xác minh từ log | FAIL do cấu hình profile, không phải một lượt test API 26 PASS |
| CI sau bản sửa | INCOMPLETE — chưa commit/push hoặc chạy lại |

Parser cài trong thư mục tạm ngoài repo, không thêm dependency dự án. Không chạy
actionlint, Gradle hoặc emulator/instrumentation trong lượt này. Không commit/
push, rerun workflow hoặc thay dữ liệu AVD. Kết luận phát hành vẫn **Not Ready**;
chờ run mới của workflow đã sửa để xác nhận API 26 startup/migration và CI.
