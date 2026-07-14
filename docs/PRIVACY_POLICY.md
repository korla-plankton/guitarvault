# Privacy Policy for De-GAS

**Last updated: July 12, 2026**

De-GAS ("Digital Guitar Asset System") is a mobile application that helps guitar collectors manage their collections. This Privacy Policy explains how the app handles your data.

## 1. Data We Collect

### 1.1 Data You Provide
- **Guitar specifications**: Brand, model, year, serial number, body/neck woods, pickups, hardware, and other details you enter about your guitars
- **Photos**: Images captured with your device camera or pasted from your clipboard
- **Valuation data**: Purchase prices, current values, insurance information
- **Condition and maintenance records**: Condition ratings, maintenance logs
- **Custom fields**: Any custom data you add to guitars
- **Wishlist items**: Guitars you want to acquire

### 1.2 Data Automatically Collected
- **Camera access**: Only when you actively use the in-app camera to photograph guitars
- **Clipboard access**: Only when you tap "Paste Photo" — reads image data from your clipboard

### 1.3 Data We Do NOT Collect
- We do **not** collect your name, email, phone number, or any personal identifying information
- We do **not** track your location
- We do **not** access your contacts, accounts, or other apps
- We do **not** use analytics, advertising, or tracking SDKs
- We do **not** sell, rent, or share your data with any third party

## 2. How Your Data Is Stored

### 2.1 Local Storage
All your data — guitar specs, photos, valuations, and everything else — is stored **entirely on your device** in a local JSON file within the app's private storage area. Your data never leaves your device unless you manually export it.

### 2.2 No Cloud Sync
De-GAS does not use cloud servers, databases, or sync. There is no account system. Your collection exists only on the device where you created it.

### 2.3 Backup and Export
You can export your collection as a JSON file at any time. This export is saved to a location you choose. You are responsible for the security of any exported files.

### 2.4 On-Device AI Processing
The app includes ML Kit Subject Segmentation for background removal in photos. This AI processing runs **entirely on your device** — no images are sent to any server for processing. The ML Kit model is downloaded via Google Play services on first use.

## 3. Data Permissions

The app requests the following Android permissions:

| Permission | Purpose | When Used |
|------------|---------|-----------|
| Camera | Capture photos of your guitars | Only when you tap "Add Photo" → camera |
| Internet | Download ML Kit AI model; open web searches for specs | Background removal; spec search buttons |
| FOREGROUND_SERVICE | Run AI processing without interruption | During background removal only |
| POST_NOTIFICATIONS | Show processing status notification | During background removal only |
| READ_MEDIA_IMAGES | Read pasted images | Only when you tap "Paste Photo" |

## 4. Data Security

Your data is stored in the app's private storage area, which is isolated from other apps on Android. The app does not transmit any data over the internet except:
- Web browser links when you tap "Search Google/Reverb/eBay" (opens your browser)
- ML Kit model download (handled by Google Play services)

## 5. Data Retention

Your data is retained on your device for as long as the app is installed. If you uninstall the app, all data is permanently deleted. There are no server-side copies.

## 6. Your Rights

- **Access**: You can view all your data within the app at any time
- **Export**: You can export your entire collection as a JSON file
- **Delete**: You can delete individual guitars or uninstall the app to remove all data
- **No account needed**: There is no account to delete because we don't collect account information

## 7. Children's Privacy

The app is not directed at children under 13. We do not knowingly collect data from children. If you believe a child has provided data through the app, contact us and we will delete it.

## 8. Third-Party Services

- **Google Play Services (ML Kit)**: Used for on-device subject segmentation (background removal). Google's privacy policy applies to the model download process. No image data is transmitted.
- **Google Play Store**: Standard app distribution. Google's privacy policy applies to the install/update process.
- **Coil (image library)**: Open-source library that loads images within the app. No data transmitted.

## 9. Changes to This Policy

We may update this Privacy Policy from time to time. Any changes will be posted within the app and on this page with an updated date.

## 10. Contact

For privacy questions or requests, please contact:
- GitHub: [korla-plankton/de-gas](https://github.com/korla-plankton/de-gas)

## 11. Open Source Components

De-GAS uses the following open-source libraries:
- Jetpack Compose (Apache 2.0)
- CameraX (Apache 2.0)
- ML Kit Subject Segmentation (Google)
- kotlinx.serialization (Apache 2.0)
- Coil (Apache 2.0)
- Navigation Compose (Apache 2.0)
- ExifInterface (Apache 2.0)
