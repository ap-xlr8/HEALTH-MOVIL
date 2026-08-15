# Health OS - Módulo Móvil (Android)

## Propósito del Módulo
Este módulo contiene la aplicación nativa en Android para Health OS. Su objetivo principal es servir como la interfaz primaria de interacción tanto para **Pacientes** como para **Cuidadores**. Permite el monitoreo continuo de métricas de salud, la sincronización de datos biométricos desde wearables (vía Bluetooth Low Energy), la gestión de medicamentos, alertas en tiempo real, y análisis preventivo usando Machine Learning (on-device ML). Es 100% autónomo para el equipo Android.

## Tecnologías y Setup Local

### Tech Stack
- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Base de Datos Local:** Room + SQLite (con SQLCipher para cifrado)
- **Inyección de Dependencias:** Hilt
- **Cliente HTTP:** Retrofit + OkHttp
- **Asincronía y Flujos:** Coroutines + StateFlow / SharedFlow
- **Wearables:** Bluetooth Low Energy (BLE)
- **Machine Learning:** TensorFlow Lite / ONNX (Inferencia On-Device)
- **Sincronización:** WorkManager (Outbox Pattern)
- **Seguridad:** EncryptedSharedPreferences, BiometricPrompt

### Setup Local
1. **Instalar Android Studio:** Versión Iguana o superior.
2. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/healthos/healthos-android.git
   ```
3. **Abrir el proyecto:** Navegar a la carpeta `MOVIL` en Android Studio.
4. **Sincronizar Gradle:** Haz clic en "Sync Project with Gradle Files".
5. **Configurar el Emulador:**
   - Crear un AVD (Android Virtual Device) con API nivel 26+ (recomendado API 34).
   - Para conectar al backend local, la app usa `http://10.0.2.2:8080`.
6. **Ejecutar App:**
   ```bash
   ./gradlew installDebug
   ```

## Variables de Entorno (BuildConfig Fields)

Las variables se configuran en el archivo `local.properties` para desarrollo, o mediante GitHub Secrets en CI/CD.

| Nombre | Descripción | Ejemplo de Valor | Requerido |
|--------|-------------|------------------|-----------|
| `API_BASE_URL_DEV` | URL del backend local (emulador). | `http://10.0.2.2:8080` | Sí |
| `API_BASE_URL_STAGING` | URL del backend en Staging. | `https://staging.api.healthos.app` | Sí |
| `API_BASE_URL_PROD` | URL del backend en Producción. | `https://api.healthos.app` | Sí |
| `SENTRY_DSN` | DSN para monitoreo de crashes en Android. | `https://example@sentry.io/12345` | Opcional |
| `DB_PASSPHRASE` | Contraseña para encriptar la DB Room local. | `dev-secret-key-123` | Sí |

## Estructura de Carpetas

Aplicamos **Clean Architecture** con separación estricta:

- `domain/` — Modelos de negocio y casos de uso. Kotlin puro, sin dependencias de Android SDK. 100% testeable en JVM.
- `data/` — Implementación de repositorios, entidades Room, DAOs, DTOs de Retrofit.
- `presentation/` — Vistas en Jetpack Compose, ViewModels, estados de UI y navegación. Dividido en Patient y Caregiver.
- `bluetooth/` — Manejo de BLE. Contiene escáner y adapters por fabricante (GenericBLEAdapter, XiaomiBandAdapter, GarminAdapter).
- `sync/` — Implementación del patrón Outbox. Contiene SyncEngine y Workers.
- `ml-runtime/` — Clases para cargar y ejecutar inferencias (TFLite).
- `security/` — Utilidades de Keystore, cifrado local, manejo de JWT tokens.

## Checklist del Equipo Móvil

- [ ] Configurar inyección de dependencias (Hilt) en la capa de aplicación.
- [ ] Implementar la base de datos cifrada con Room + SQLCipher.
- [ ] Construir la navegación principal diferenciada (Paciente con 5 tabs vs Cuidador con 4 tabs).
- [ ] Desarrollar los flujos de autenticación JSON y almacenamiento en Keystore.
- [ ] Implementar cliente de red con Retrofit e interceptores para inyectar JWT.
- [ ] Desarrollar escáner BLE y flujos de conexión GATT genérica.
- [ ] Implementar Adapters de hardware: XiaomiBand y Garmin.
- [ ] Integrar lector QR (CameraX) para vinculación de dispositivos.
- [ ] Integrar WorkManager para la sincronización Outbox (canales Normal y Crítico).
- [ ] Integrar modelos TFLite on-device en `ml-runtime/`.
- [ ] Implementar UI pantallas Paciente (Dashboard, Métricas, SOS, etc).
- [ ] Implementar UI pantallas Cuidador (Lista pacientes, Detalle).
- [ ] Configurar ktlint, detekt, tests unitarios (JUnit5), Maestro/Espresso, y GitHub Actions.

## Pantallas, Endpoints y Modelos

La app consume JSON. NUNCA cookies. El `access_token` va en el header `Authorization: Bearer <token>`.

### Módulo AUTH

#### A1. Bienvenida
- **Propósito:** Splash y opciones iniciales de sesión.
- **Datos que muestra:** Branding, botones Login/Registro.
- **Estados:** Vacío. No requiere API.

#### A2. Registro
- **Propósito:** Creación de cuenta de usuario.
- **Endpoints Consume:** `POST /v1/auth/register`
- **cURL:**
```bash
curl -X POST http://10.0.2.2:8080/v1/auth/register \
-H "Content-Type: application/json" \
-d '{
  "email": "paciente@ejemplo.com",
  "password": "Password123!",
  "role": "PATIENT",
  "first_name": "Carlos",
  "last_name": "López"
}'
```
- **Campos de Respuesta:**
| Nombre | Tipo | Nullable | Descripción |
|--------|------|----------|-------------|
| `id` | String | No | UUID del usuario |
| `email` | String | No | Correo registrado |
| `message` | String | No | Confirmación de registro |
- **Loading:** CircularProgressIndicator en botón de registrar.
- **Error:** Toast o Snackbar con mensaje ("El email ya existe").

#### A3. Login
- **Propósito:** Iniciar sesión y obtener token JWT.
- **Endpoints Consume:** `POST /v1/auth/login`
- **cURL:**
```bash
curl -X POST http://10.0.2.2:8080/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "paciente@ejemplo.com",
  "password": "Password123!"
}'
```
- **Campos de Respuesta:**
| Nombre | Tipo | Nullable | Descripción |
|--------|------|----------|-------------|
| `access_token` | String | No | JWT para API |
| `refresh_token` | String | No | JWT para renovar sesión |
| `role` | String | No | "PATIENT" o "CAREGIVER" |

#### A4. Verificación Email
- **Propósito:** Validar OTP.
- **Endpoints Consume:** `POST /v1/auth/verify-email`
- **cURL:**
```bash
curl -X POST http://10.0.2.2:8080/v1/auth/verify-email \
-H "Content-Type: application/json" \
-d '{ "email": "paciente@ejemplo.com", "code": "847291" }'
```

#### A5. Recuperar Contraseña
- **Propósito:** Enviar email de reset.
- **Endpoints Consume:** `POST /v1/auth/forgot-password`
- **cURL:**
```bash
curl -X POST http://10.0.2.2:8080/v1/auth/forgot-password \
-H "Content-Type: application/json" \
-d '{ "email": "paciente@ejemplo.com" }'
```

#### A6. Onboarding
- **Propósito:** Perfil de salud inicial (peso, altura).
- **Endpoints Consume:** `PUT /v1/patients/me/health-profile`
- **cURL:**
```bash
curl -X PUT http://10.0.2.2:8080/v1/patients/me/health-profile \
-H "Authorization: Bearer eyJ..." \
-H "Content-Type: application/json" \
-d '{
  "weight_kg": 75.5,
  "height_cm": 180,
  "blood_type": "O+"
}'
```

### Módulo PACIENTE

#### P1. Dashboard
- **Propósito:** Resumen general del paciente actual.
- **Endpoints Consume:** `GET /v1/measurements/latest`, `GET /v1/medications` (hoy), `GET /v1/alerts` (activas)
- **cURL:**
```bash
curl -X GET "http://10.0.2.2:8080/v1/measurements/latest" \
-H "Authorization: Bearer eyJ..."
```
- **Campos de Respuesta:**
| Nombre | Tipo | Nullable | Descripción |
|--------|------|----------|-------------|
| `id` | String | No | ID medición |
| `metric_type` | String | No | "HEART_RATE", "SpO2" |
| `value` | Double | No | Valor medido |
| `unit` | String | No | "bpm", "%" |
| `timestamp` | String | No | ISO8601 |
- **Loading/Error/Vacío:** Mostrar Skeleton Loaders al iniciar. En caso de error, botón "Reintentar conexión".

#### P2. Métricas
- **Propósito:** Histórico de métricas en gráficas.
- **Endpoints Consume:** `GET /v1/measurements`
- **cURL:**
```bash
curl -X GET "http://10.0.2.2:8080/v1/measurements?metric=HEART_RATE&days=7" \
-H "Authorization: Bearer eyJ..."
```

#### P8. Botón SOS
- **Propósito:** Lanzar alerta crítica inmediata.
- **Endpoints Consume:** `POST /v1/alerts/sos`
- **cURL:**
```bash
curl -X POST "http://10.0.2.2:8080/v1/alerts/sos" \
-H "Authorization: Bearer eyJ..." \
-H "Content-Type: application/json" \
-d '{
  "location": { "lat": 19.4326, "lng": -99.1332 },
  "trigger": "MANUAL_BUTTON"
}'
```

#### P9. Dispositivos
- **Propósito:** Ver, vincular y desvincular wearables (lectura QR CameraX).
- **Endpoints Consume:** `GET /v1/devices`, `POST /v1/devices`, `DELETE /v1/devices/:id`
- **cURL (POST):**
```bash
curl -X POST "http://10.0.2.2:8080/v1/devices" \
-H "Authorization: Bearer eyJ..." \
-H "Content-Type: application/json" \
-d '{
  "device_id": "AA:BB:CC:DD:EE:FF",
  "model": "Xiaomi Band 8",
  "protocol": "PROPRIETARY_XIAOMI",
  "public_key": "MIIBIjANBgkqhkiG9w0..."
}'
```

### Módulo CUIDADOR

#### C1. Lista de pacientes
- **Propósito:** Muestra los pacientes que el cuidador monitorea.
- **Endpoints Consume:** `GET /v1/caregiver/patients`, `GET /v1/measurements/latest`
- **cURL:**
```bash
curl -X GET "http://10.0.2.2:8080/v1/caregiver/patients" \
-H "Authorization: Bearer eyJ..."
```
- **Campos de Respuesta:**
| Nombre | Tipo | Nullable | Descripción |
|--------|------|----------|-------------|
| `id` | String | No | ID Paciente |
| `first_name` | String | No | Nombre |
| `status` | String | No | Estado "NORMAL", "ALERT" |

#### C2. Detalle del paciente
- **Propósito:** Vista profunda de datos de un paciente específico.
- **Endpoints Consume:** `GET /v1/patients/:id`
- **cURL:**
```bash
curl -X GET "http://10.0.2.2:8080/v1/patients/b382d2-..." \
-H "Authorization: Bearer eyJ..."
```

## Sincronización: Outbox Pattern

El Outbox pattern asegura la entrega de datos, guardando métricas en Room SQLite y enviando al backend usando WorkManager. Esto garantiza la persistencia sin importar conectividad.

### Ejemplo de SyncEngine (Kotlin)
```kotlin
package com.healthos.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncEngine {

    fun scheduleNormalSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<NormalSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "NORMAL_SYNC_WORK",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun triggerCriticalSync(context: Context, eventId: String) {
        val data = workDataOf("EVENT_ID" to eventId)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val criticalRequest = OneTimeWorkRequestBuilder<CriticalSyncWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(criticalRequest)
    }
}
```

## Bluetooth Low Energy (BLE)

La vinculación inicia mostrando un código QR en el reloj y usando CameraX en la app para escanear `deviceId` y `protocol`. Luego sigue la conexión GATT.

### Ejemplo de flujo BLE (Kotlin)
```kotlin
package com.healthos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

@SuppressLint("MissingPermission")
class BleConnectionManager(private val context: Context) {
    
    val connectionState = MutableStateFlow<BleState>(BleState.Disconnected)

    // 1 y 2. Escaneo y descubrimiento de dispositivo ocurre antes.
    // 3. connectGatt
    fun connectToDevice(device: BluetoothDevice) {
        connectionState.value = BleState.Connecting
        device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState.value = BleState.Connected
                // 4. discoverServices
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState.value = BleState.Disconnected
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                
                // 5. setCharacteristicNotification
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        // 6. Parsear bytes -> Room
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val flag = characteristic.properties
                val format = if ((flag and 0x01) != 0) BluetoothGattCharacteristic.FORMAT_UINT16 else BluetoothGattCharacteristic.FORMAT_UINT8
                val heartRate = characteristic.getIntValue(format, 1)
                
                // Guardar lectura en DB (Room) para ser procesada por Outbox WorkManager
                saveMeasurementToRoom(heartRate)
            }
        }
    }
    
    private fun saveMeasurementToRoom(value: Int) {
        // Inserción en Room. Estado inicial: PENDING
    }
}
```

## DevSecOps y CI/CD

El código móvil tiene verificaciones estrictas automatizadas.

### Pipeline Steps (GitHub Actions)
1. **Linting:** 
   ```bash
   ./gradlew ktlintCheck detekt
   ```
2. **Android Lint:** Análisis de UI y accesibilidad.
   ```bash
   ./gradlew lintRelease
   ```
3. **Unit Tests:** Pruebas limpias (sin emulador) usando `kotlinx-coroutines-test`.
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **UI Tests:** Se ejecuta Maestro en un emulador Android nativo.
5. **Build APK/AAB:** Firmado con llaves en GitHub Secrets.

### Reglas de Seguridad Específicas
- **Tokens:** Guardados en `EncryptedSharedPreferences` (AES256-GCM). Nunca persistidos en texto plano.
- **Base de Datos:** Datos de salud usan **SQLCipher**.
- **Red:** `Network Security Config` configurado. Solo `https://` y Certificate Pinning activado en Prod/Staging.
- **Biometría:** Requiere `BiometricPrompt` para desbloqueo si la sesión estuvo inactiva.
- **Logs y Sentry:** Logcat filtrado (ProGuard asume log stripping en release). Sentry SDK configurado para NO mandar PII ni datos médicos clínicos.

---

## ⚠️ NOTAS DE COORDINACION DE EQUIPO — LEER ANTES DE ARRANCAR

> Puntos donde el equipo de Mobile depende de otros equipos. No esperes a que esten listos para empezar, pero ten en cuenta cuando llegue el momento de integrar.

---

### [DEPENDE DEL BACKEND] No generes el cliente de API a mano — espera el openapi.yaml

**El cliente de API del proyecto se genera automaticamente** a partir del `openapi.yaml` que publica el equipo de Backend. Si lo escribes a mano, va a haber diferencias con la implementacion real.

**Que hacer mientras el backend no tiene el contrato listo:**
- Usa MSW (Mock Service Worker) o un servidor mock local para desarrollar todas las pantallas
- Trabaja con datos de prueba locales en Room
- Cuando el backend publique el contrato, ejecuta la generacion y tu mock se reemplaza por el cliente real

**Cuando el backend avise que el contrato esta listo:**
```bash
# Generar cliente automaticamente desde el contrato del backend
# (agrega este comando al build.gradle o ejecutalo manualmente)
./gradlew generateApiClient \
  --spec=https://staging.api.healthos.app/v1/openapi.yaml \
  --output=data/remote/generated/

# NUNCA editar los archivos en data/remote/generated/ manualmente
# Se sobreescriben en cada generacion
```

---

### [DEPENDE DEL BACKEND] Formato de eventos WebSocket — no lo implementes hasta tenerlo definido

El equipo de Backend debe publicar el `asyncapi.yaml` con el schema exacto de cada evento WebSocket antes de que implementes el listener en la app.

**Los eventos esperados son:**
```
measurement.ingested  → actualizar tarjeta de metrica en P1 Dashboard
alert.created         → mostrar banner en P4 Alertas
health.event.critical → activar pantalla de emergencia
consent.updated       → refrescar permisos en P14 Consentimientos
```

**Mientras no tengas el schema exacto:**
- Implementa el WebSocket con un handler generico que loguea el evento recibido
- NO hardcodees los nombres de campos — usa el schema cuando este disponible
- El dia que el backend publique el asyncapi.yaml, el listener se escribe en menos de 2 horas

---

### [ACCION UNICA DEL EQUIPO] URL de staging — solo necesitas pedirla una vez

Las URLs en este README son placeholders. Cuando el equipo de infra/backend configure Render, te dara una URL real. Guardala en tu `.env.staging` y no la vuelvas a preguntar.

```
# .env.staging (una vez que el equipo de backend la comparta)
API_BASE_URL=https://[nombre-real].onrender.com
WS_BASE_URL=wss://[nombre-real].onrender.com
```

---

### [EXPECTATIVA REALISTA] Los adapters BLE de dispositivos comerciales van a requerir iteraciones

**Esta es la parte mas impredecible del proyecto.**

Los protocolos BLE de Xiaomi, Garmin y otros fabricantes **no son completamente publicos**. El README documenta lo que se sabe por ingenieria inversa, pero:

- Los fabricantes cambian el protocolo con actualizaciones de firmware sin aviso
- Los bytes que documenta la especificacion a veces no son los que manda el dispositivo real
- Algunos dispositivos mandan datos en formato distinto segun la region o el modelo exacto

**Que esperar:**
- El GenericBLEAdapter (GATT estandar) deberia funcionar sin sorpresas
- El XiaomiBandAdapter y GarminAdapter van a necesitar 2-3 iteraciones con el dispositivo fisico en mano
- Necesitas tener los dispositivos fisicos para probar — el emulador no puede simular BLE real de forma confiable

**Como manejarlo:**
1. Implementa el adapter segun la documentacion del README
2. Prueba con el dispositivo fisico y captura el trafico BLE con nRF Connect (app Android gratuita)
3. Ajusta el parser segun los bytes reales que ves en la captura
4. Documenta los cambios en `protocols/[fabricante]/` para que el equipo tenga el protocolo real actualizado

Esto no es un error de documentacion — es la naturaleza del trabajo con hardware propietario.

---
