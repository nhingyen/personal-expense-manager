# TASK-014: Kiểm thử Signed Upgrade v2 → v3

Quy trình giữ dữ liệu thật của bản Release trong AVD riêng. Không chạy
`connectedDebugAndroidTest`, `am instrument`, uninstall, clear storage hoặc wipe
AVD ở bất kỳ bước nào. Không dùng Pixel_5 đang cài Debug để thử signed upgrade.

**Kết quả mới nhất ngày 2026-10-28: PASS v2 → v3 trên AVD API 35 riêng.**
Đã xác minh lại v2 trước cài, cập nhật bằng `install -r`, so sánh dữ liệu sau cài
và sau cold launch, kiểm tra UI và logo trên Home Screen/App Drawer. Bằng chứng
chi tiết ở phần kết quả thực thi cuối tài liệu. Các bước chuẩn bị/chờ ký bên
dưới là lịch sử; không cài lại v2 hoặc tạo lại dữ liệu trên AVD hiện đã lên v3.

## Artifact và thiết bị

- Package: `com.ngyennhi.expensemanager`; versionName cả hai bản: `1.0.0`.
- v2 đã ký, lưu riêng và chỉ đọc:
  `app/release/upgrade-verification/v2/app-v1.0.0-vc2-release.apk`.
- SHA-256 v2:
  `AF23C7B1998250EDAB65DB233650AD495DF05666C3F8B41C6144865DD9474E69`.
- Certificate SHA-256 Release phải khớp ở cả v2 và v3:
  `bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.
- v3 unsigned đã build:
  `app/build/outputs/apk/release/app-release-unsigned.apk`; bản sao độc lập:
  `app/release/upgrade-verification/v3-unsigned/app-v1.0.0-vc3-unsigned.apk`.
- AVD riêng: `ExpenseManager_SignedUpgrade_API35`, Google APIs x86_64/API 35,
  cổng 5558 (`emulator-5558` trong lượt chuẩn bị này). Các lần sau luôn kiểm tra
  tên AVD, không dựa vào serial một mình.
- Artifact, log và snapshot chứa dữ liệu mẫu chỉ lưu dưới `app/release/`, được
  Git ignore. Không đặt bản v2 duy nhất trong `app/build/` vì clean/build có thể
  ghi đè hoặc xóa output đó. File chỉ đọc là lớp bảo vệ bổ sung, không phải
  kho lưu trữ bất biến; giữ hash để kiểm tra lại.

## Ký v3 trong Android Studio — người dùng thực hiện

Source hiện tại đã đặt `versionCode = 3`, `versionName = "1.0.0"`. Sync project,
chọn **Build → Generate Signed App Bundle or APK → APK**, module `app`; chọn
keystore và alias Release cũ, tự nhập mật khẩu trong IDE. Chọn variant `release`
và thư mục đích riêng `app/release/upgrade-verification/v3-signed` rồi tạo APK.
Không tạo key mới hoặc thay cấu hình ký trong Gradle. Thư mục con/tên APK thực tế
do IDE hiển thị sau build; dùng đúng đường dẫn đó ở bước xác minh.
Tham khảo [Android: Sign your app with your key](https://developer.android.com/studio/publish/app-signing#sign_release).

Không cài file `*-unsigned.apk`. Không cần đưa keystore, alias/password hoặc
thông tin đăng nhập vào báo cáo; chỉ cần đường dẫn APK đã ký để tiếp tục.

## Xác minh và khóa đúng thiết bị trước khi cài

Ví dụ PowerShell tại thư mục gốc repo; chỉnh đường dẫn APK v3 theo output IDE:

```powershell
$taskSdk = 'C:/Users/Admin/AppData/Local/Android/Sdk'
$taskAdb = "$taskSdk/platform-tools/adb.exe"
$taskSigner = "$taskSdk/build-tools/36.0.0/apksigner.bat"
$taskAapt = "$taskSdk/build-tools/36.0.0/aapt.exe"
$taskSerial = 'emulator-5558'
$taskV2 = 'app/release/upgrade-verification/v2/app-v1.0.0-vc2-release.apk'
$taskV3 = 'app/release/upgrade-verification/v3-signed/release/app-release.apk'
$taskEvidence = 'app/release/upgrade-verification'
& $taskAdb -s $taskSerial shell getprop ro.boot.qemu.avd_name
& $taskAdb -s $taskSerial shell getprop sys.boot_completed
& $taskSigner verify --verbose --print-certs $taskV2
& $taskSigner verify --verbose --print-certs $taskV3
& $taskAapt dump badging $taskV2
& $taskAapt dump badging $taskV3
Get-FileHash -LiteralPath $taskV2,$taskV3 -Algorithm SHA256
```

Dừng nếu chữ ký không verify, certificate khác fingerprint Release ở trên,
package/versionName khác, versionCode không đúng 2/3, hoặc AVD không đúng tên/
chưa boot. Lưu output công khai vào evidence. Không giải quyết signature mismatch
bằng uninstall. Lượt này giữ Room schema v2; versionCode 3 là phiên bản APK,
không phải schema database.

## Cài v2 và tạo baseline — chỉ lần đầu trên AVD riêng

Chỉ khởi động AVD riêng nếu nó chưa chạy; không mở bản thứ hai cùng userdata:

```powershell
# Khi AVD chưa chạy, chạy lệnh này trong terminal riêng và chờ boot hoàn tất.
& "$taskSdk/emulator/emulator.exe" -avd ExpenseManager_SignedUpgrade_API35 -port 5558 -no-snapshot -gpu host -feature -Vulkan
```

Kiểm tra package trước khi cài lần đầu. Nếu đã có v2 cùng signer với dữ liệu
mẫu, giữ nguyên và tiếp tục chụp baseline; nếu có bản khác, dừng để kiểm tra:

```powershell
& $taskAdb -s $taskSerial shell pm list packages -u com.ngyennhi.expensemanager
& $taskAdb -s $taskSerial install -r $taskV2
& $taskAdb -s $taskSerial shell am start -W -n com.ngyennhi.expensemanager/.MainActivity
```

Qua UI v2, chọn English và Light để có lựa chọn đã lưu, tạo mẫu sau với ngày
**2026-10-28**, tháng ngân sách **2026-10** (nếu ngày thiết bị khác, chọn ngày
thủ công trong form). Không chèn SQL hoặc sửa preference XML để seed:

| Dữ liệu | Giá trị mẫu |
| --- | --- |
| Wallet `UpgradeCash`, Cash | Opening 1.000.000 VND |
| Wallet `UpgradeBank`, Bank | Opening 2.000.000 VND |
| Income, Salary, `UpgradeIncome` | 500.000 VND → UpgradeCash |
| Expense, Food, `UpgradeFood` | 125.000 VND → UpgradeCash |
| Expense, Travel, `UpgradeTravel` | 200.000 VND → UpgradeBank |
| Budget tháng 2026-10 | 1.000.000 VND |

Đợi từng thao tác Save hoàn tất. Wallet mặc định vẫn giữ nguyên. Kỳ vọng:
UpgradeCash **1.375.000**, UpgradeBank **1.800.000**, tổng ví **3.175.000**;
tháng 10 thu **500.000**, chi **325.000**, net **175.000**; ngân sách còn
**675.000** (tỷ lệ chính xác 32,5%; UI v2 hiển thị `Used 32%`). Ghi ID thực tế bằng snapshot, không giả định ID
chỉ từ thứ tự tạo. Chụp Home, Wallets, wallet details, transaction details,
Budget và Settings để đối chiếu UI.

## Snapshot trước upgrade

Release không debuggable, vì vậy `run-as`/Database Inspector không phải phương
án mặc định. Dùng `adb root` **chỉ trên AVD Google APIs dành riêng này**, không
đổi APK sang Debug và không root thiết bị cá nhân. Nếu image không hỗ trợ root,
đánh dấu kiểm tra Room/IDs/sequence là INCOMPLETE; ảnh UI không thay thế bằng
chứng dữ liệu nội bộ.

```powershell
& $taskAdb -s $taskSerial root
& $taskAdb -s $taskSerial wait-for-device
python -B scripts/signed_upgrade_snapshot.py capture --adb $taskAdb --serial $taskSerial --apk $taskV2 --expected-version 2 --out "$taskEvidence/before-v2"
```

Script chỉ cho phép AVD đúng tên, kiểm tra boot/root, versionCode và hash APK
đang cài khớp artifact được chỉ định. Sau đó force-stop app để ổn định dữ liệu,
sao chép nguyên database cùng WAL/SHM và SharedPreferences, đọc SQLite trên bản
sao máy tính; không ghi vào storage app. Thư mục output phải chưa tồn tại.
App được giữ ở trạng thái stopped sau capture, không mất dữ liệu.

Snapshot ghi user_version, Room identity/schema, mọi cột của wallets/transactions,
IDs/wallet_id, foreign keys/check, sqlite_sequence, integrity, số dư tính từ
opening + income − expense, preference XML theo nội dung và app locale.
UID/package/AVD và hash file thô cũng được ghi; không dựa vào hash toàn file DB
để kết luận vì WAL/checkpoint có thể làm thay đổi bố cục byte.

## Cài v3 tại chỗ và so sánh

Chỉ làm sau khi v3 đã ký được xác minh cùng certificate và baseline đã lưu:

```powershell
& $taskAdb -s $taskSerial install -r $taskV3
& $taskAdb -s $taskSerial shell dumpsys package com.ngyennhi.expensemanager
python -B scripts/signed_upgrade_snapshot.py capture --adb $taskAdb --serial $taskSerial --apk $taskV3 --expected-version 3 --out "$taskEvidence/after-v3-install"
python -B scripts/signed_upgrade_snapshot.py compare --before "$taskEvidence/before-v2" --after "$taskEvidence/after-v3-install"
& $taskAdb -s $taskSerial shell am start -W -n com.ngyennhi.expensemanager/.MainActivity
```

Đối chiếu các màn hình và số liệu kỳ vọng, quan hệ ví trong từng transaction,
ngôn ngữ/theme và ngân sách. Xem App Drawer và shortcut Home Screen: logo ví tím
mới phải xuất hiện; nếu cache chưa cập nhật, mở lại launcher hoặc reboot thiết
bị, không clear app/launcher storage. Lưu ảnh trước/sau. Sau kiểm tra UI, chụp
lần nữa để xác nhận mở v3 không làm thay đổi dữ liệu:

```powershell
python -B scripts/signed_upgrade_snapshot.py capture --adb $taskAdb --serial $taskSerial --apk $taskV3 --expected-version 3 --out "$taskEvidence/after-v3-cold-launch"
python -B scripts/signed_upgrade_snapshot.py compare --before "$taskEvidence/before-v2" --after "$taskEvidence/after-v3-cold-launch"
```

PASS cần đủ: cả hai chữ ký đúng/cùng certificate, install -r Success, versionCode
3 cùng UID, hai lần so sánh dữ liệu PASS, UI totals/preferences và logo PASS.
Nếu mới có build unsigned, baseline hoặc test công cụ trên máy tính, signed
upgrade vẫn **INCOMPLETE**. Ghi mọi khác biệt trước khi sửa; giữ nguyên dữ liệu
AVD để điều tra. API 26 migration/startup và remote CI là gate riêng của TASK-014.

Kiểm tra công cụ trên máy tính, không tiếp xúc emulator:

```powershell
python -B -m unittest discover -s scripts -p test_signed_upgrade_snapshot.py -v
```

## Mốc chuẩn bị ngày 2026-10-28 — trước khi có APK v3 đã ký

v2 verify/archive, v3 unsigned build và baseline trên AVD riêng đã PASS. Baseline
đã tồn tại tại `app/release/upgrade-verification/before-v2/`; không chạy lại seed
hoặc ghi đè thư mục này. Wallet IDs 1/2/3 là Default/UpgradeCash/UpgradeBank;
transaction IDs 1/2/3 liên kết tới wallet 2/2/3. Package UID 10209, Room schema 2,
identity `ffda5e095f46485f8c26d51bae071165`, hai sequence đều 3, foreign_key_check
rỗng, integrity_check `ok`. Theme `LIGHT`, locale `en`, budget tháng 2026-10 là
1.000.000. Snapshot JSON SHA-256:
`ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f`.

App v2 đã force-stop sau capture và dữ liệu được giữ nguyên trên AVD. Bước tiếp
theo là người dùng ký v3, sau đó verify và tiếp tục từ phần cài v3 tại chỗ.
Ảnh `app/release/upgrade-verification/emulator/v2-app-drawer.png` xác nhận v2
archive vẫn dùng icon Android mẫu cũ. Đây là baseline để đối chiếu việc đổi sang
logo ví tím trong v3; logo mới đã có trong APK v3 unsigned.
Signed v3, hai lần compare sau upgrade, UI sau upgrade và icon trên bản v3 này
vẫn **INCOMPLETE**. Kiểm tra công cụ host **7/7 PASS** không thay thế các bước đó.

## Kết quả thực thi Signed Upgrade — 2026-10-28

**PASS** cho APK/AVD và dữ liệu mẫu xác định dưới đây. Gate signed upgrade của
TASK-014 được đóng cho API 35; API 26 startup/migration và remote CI vẫn là các
gate riêng chưa hoàn tất. Không có FAIL trong lượt thực thi này.

APK v3 người dùng cung cấp:
`app/release/upgrade-verification/v3-signed/release/app-release.apk`.
`apksigner verify --verbose --print-certs` xác minh lại cả v2/v3 thành công,
mỗi APK có một signer và signature scheme v2=true, cùng certificate SHA-256
`bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.
Metadata v3 là package `com.ngyennhi.expensemanager`, versionCode 3,
versionName 1.0.0; signed APK SHA-256:
`1DD171FB8BE0118A37B5C611B4162BBBECD608891B96E54FFBFB659B0E8BAD79`.
Archive v2 vẫn nguyên hash và được giữ riêng.

Tất cả evidence của lượt chạy ở
`app/release/upgrade-verification/run-v2-v3-20261028/` (Git ignore):

| Mốc | Kiểm tra thực tế | Kết quả |
| --- | --- | --- |
| Preflight v2 | Đúng `ExpenseManager_SignedUpgrade_API35`, `emulator-5558`, boot hoàn tất; APK đang cài khớp archive v2, code 2, UID 10209. Chụp snapshot mới và so với bản gốc: toàn bộ state, metadata và hash file thô khớp. | PASS |
| Cập nhật tại chỗ | `adb -s emulator-5558 install -r app/release/upgrade-verification/v3-signed/release/app-release.apk` trả Success, exit 0; code lên 3, name 1.0.0, UID vẫn 10209. | PASS |
| Ngay sau cài | Capture `after-v3-install`; `compare` với `before-v2` PASS, gồm toàn bộ Room/SharedPreferences/locale và số dư. | PASS |
| Cold launch/UI | `am start -W` trả Status ok, LaunchState COLD. Home/Budget/Wallets và cả ba transaction details khớp mẫu; Settings chọn English/Light. | PASS |
| Sau cold launch/kiểm tra UI | Capture `after-v3-cold-launch`; `compare` với `before-v2` PASS lần nữa. | PASS |
| Launcher | Xem ảnh thực tế thấy ví tím trên Home Screen và App Drawer; icon Android mẫu của v2 được thay. Nhấn icon mở được app. Không cần clear launcher hoặc reboot. | PASS |

Giá trị được giữ nguyên qua cả hai lần so sánh:

- Wallet IDs 1/2/3; transaction IDs 1/2/3 → wallet IDs 2/2/3; mọi cột của bản
  ghi (type, amount, category, date, note, opening balance) đều khớp.
- Số dư ví 0 / 1.375.000 / 1.800.000, tổng 3.175.000; thu 500.000, chi 325.000,
  net 175.000. Budget tháng 2026-10 giữ 1.000.000, remaining 675.000.
- Room user_version 2, identity `ffda5e095f46485f8c26d51bae071165`, schema/foreign
  keys nguyên vẹn; `sqlite_sequence` wallets/transactions 3/3,
  `foreign_key_check` rỗng, `integrity_check=ok`.
- SharedPreferences giữ đúng nội dung `appearance.xml` (`LIGHT`) và
  `monthly_budgets.xml`; platform app locale vẫn `en`.

| Snapshot JSON | SHA-256 |
| --- | --- |
| Original `before-v2` và `preflight-v2` | `ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f` |
| `after-v3-install` | `8364d8529c43c2f0a766e0a3b6ca32b977ac4e4b916133c8fa49afb1bd9286e0` |
| `after-v3-cold-launch` | `9864a955c56e02af4172a4993fd3850368eae66e093a0c7cf46d10830f2520e4` |

Hash JSON sau upgrade khác vì metadata APK/version thay đổi. Ngay sau install,
hash toàn bộ private file còn khớp baseline. Sau cold launch, riêng WAL/SHM có
hash khác; dữ liệu Room theo nội dung vẫn khớp hoàn toàn, main DB/preferences
không có khác biệt hash. Không coi hash WAL/SHM thay đổi là mất dữ liệu.

Ảnh/XML: `ui/v3-home.png`, `v3-budget.png`, `v3-settings.png`, `v3-wallets.png`,
`v3-transactions.png`, `v3-detail-Upgrade*.png`, `v3-home-screen.png`,
`v3-app-drawer.png`, `v3-launch-from-icon.png`. Log và tổng hợp:
`artifact-verification.json`, `preflight-comparison.json`, `install-v3.txt`,
`package-after-install.txt`, `ui/cold-launch.txt`, `ui/checks.json`, `result.json`.
APK v3 đã ký chứa đủ 15 launcher WebP khớp source.

Không chạy Gradle build/test/lint hoặc instrumentation trong lượt runtime này;
không thay app code/schema/signing, không xóa dữ liệu, uninstall, wipe, clear,
truy cập keystore/mật khẩu hoặc commit/push. AVD giữ **v3 và dữ liệu mẫu**, app
được force-stop sau snapshot cuối để ổn định evidence. Không downgrade về v2.
