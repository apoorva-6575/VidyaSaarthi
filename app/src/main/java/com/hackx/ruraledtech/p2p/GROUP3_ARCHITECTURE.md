# Rural EdTech: Group 3 Architecture (Learning Mesh)

The Group 3 architecture is designed to operate completely independently of the internet. It transforms every Android device running the app into a node in a decentralized mesh network. 

## 1. Store-and-Forward CDN
Instead of relying on a central server, the Learning Mesh acts as a decentralized Content Delivery Network (CDN).
- When a device installs a new educational package (via Group 1), `TransferManager` captures the event.
- It updates the local `ContentManifest` and broadcasts the new inventory to all connected peers.
- Other peers compare their local manifest against the remote manifest using `ManifestReconciler`. If they lack the content, they automatically request it. This creates a "viral" spread of educational materials through a village.

## 2. Nearby Connections Wrapper
We wrap Google's `Nearby Connections API` (specifically the `P2P_CLUSTER` strategy) inside `P2PConnectionManager` and `MeshController`.
- **Peer Authentication:** Only peers advertising themselves with the `RuralEdTech-Node` prefix are allowed to connect, mitigating unauthorized access.
- **Protocol:** All control messages (Manifests, Requests, Offers, Accepts) are serialized via `ProtocolSerializer` into lightweight JSON payloads before being transmitted as bytes.
- **Verification:** Large file transfers (`.pkg` ZIPs) bypass the JSON layer and are streamed as Payload types. We verify them using SHA-256 in `PackageVerifier` to prevent corruption or tampering before extraction.

## 3. Zip-Slip Hardening
During the extraction of received `.pkg` files, `TransferManager` protects against path traversal attacks ("Zip-Slip"). It strictly enforces that the canonical path of every extracted file remains within the designated secure temporary directory.

## 4. Passport Crypto
The Learning Passport contains sensitive student progress data.
- **Encryption:** `PassportCrypto` secures the JSON payload using AES-256-GCM.
- **Salting:** A unique 16-byte `SecureRandom` salt is generated for every encryption. The key is derived from the salt and the user's PIN via PBKDF2.
- **Decryption:** The encrypted payload, IV, and salt are packaged into a `LearningPassport` object. The receiving device must provide the same PIN to correctly derive the key and decrypt the payload.
