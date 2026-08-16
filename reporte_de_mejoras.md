# 📱 Reporte de Mejoras — Aplicación Móvil (`MOVIL`)

**Proyecto:** Health OS  
**Módulo:** `health-mobile` (Android Kotlin / Jetpack Compose / Hilt)  
**Fecha de Auditoría:** 2026-08-16  
**Auditor:** Agente de Diagnóstico Automatizado  
**Versión del Reporte:** 1.0.0

---

## 1. Auditoría de DevSecOps y Fuga de Información

### 1.1 Detección de Secretos y Datos Sensibles Hardcodeados

| Hallazgo | Severidad | Archivo | Detalle |
|:---|:---:|:---|:---|
| **URL de API hardcodeada en buildConfig** | 🟡 BAJO | `build.gradle.kts` L23-25 | `API_BASE_URL_*` con fallback a `https://health-apis.onrender.com`. Aceptable como fallback público pero idealmente debería provenir solo de `local.properties` o CI secrets. |
| **`local.properties` con SDK path** | ✅ OK | `local.properties` | Solo contiene `sdk.dir` — sin credenciales. |
| **Certificate Pins hardcodeados** | ✅ OK | `Network.kt` L165-170 | SHA-256 pins de TLS chain — práctica correcta y necesaria para certificate pinning. |
| **Modelos TFLite en assets** | 🟡 INFO | `app/src/main/assets/models/` | 5 modelos `.tflite` embebidos. No son secretos pero son propiedad intelectual. R8/ProGuard mitiga la ingeniería inversa parcialmente. |
| **Sentry DSN** | ✅ OK | `build.gradle.kts` L26 | DSN vacío por defecto, inyectado vía `local.properties` o CI. |

> [!NOTE]
> No se detectaron claves API, tokens, contraseñas ni URLs privadas hardcodeadas en código fuente. La gestión de secretos es adecuada.

### 1.2 Análisis de Prácticas DevSecOps en Pipeline CI/CD

| Práctica | Estado | Evidencia |
|:---|:---:|:---|
| Secret Scanning (Gitleaks) | ✅ Implementado | `android.yml` L13-22 |
| SCA (OWASP Dependency-Check) | ✅ Implementado | `android.yml` L24-53 — SARIF upload, falla en CVSS ≥ 7. |
| SAST (ktlint + detekt) | ✅ Implementado | `android.yml` L77-78 |
| Android Lint (seguridad + performance) | ✅ Implementado | `android.yml` L80-81 — `lintDebug`. |
| Unit Tests | ✅ Implementado | `android.yml` L83-84 — `testDebugUnitTest`. |
| Release Build (firmado) | ✅ Implementado | `android.yml` L89-103 — APK + AAB con keystore desde secrets. |
| Instrumented Tests en CI | ❌ No implementado | Solo tests unitarios JVM. Maestro y Espresso no ejecutados en CI (solo archivo `.maestro/` presente). |
| Container/Image Scan | N/A | No aplica para módulo móvil. |

### 1.3 Gestión de Variables de Entorno

| Criterio | Estado | Observación |
|:---|:---:|:---|
| `local.properties` en `.gitignore` | ✅ | Correctamente excluido. |
| `local.properties.example` disponible | ✅ | Template con placeholders documentados. |
| Keystore y signing keys en CI secrets | ✅ | `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` vía GitHub Secrets. |
| Sentry DSN vía properties/CI | ✅ | No hardcodeado; inyectado dinámicamente. |
| `network_security_config.xml` | ✅ | Cleartext traffic deshabilitado en release. Certificate pinning como respaldo adicional a nivel OkHttp. |

---

## 2. Checklist de Madurez Técnica (12 Ejes)

| # | Eje | Estatus | Observaciones |
|:---:|:---|:---:|:---|
| 1 | **Requerimientos y Arquitectura** | ✅ Cumple | Clean Architecture: `domain/` (model, repository, usecase), `data/` (local Room + remote Retrofit), `presentation/` (Compose + ViewModel). Offline-First con Room + SQLCipher como fuente de verdad. |
| 2 | **Desarrollo y Estándares de Código** | ✅ Cumple | ktlint + detekt configurados. Kotlin 1.9, Jetpack Compose con Material3, Hilt DI, Coroutines/Flow. |
| 3 | **Git y Control de Versiones** | ✅ Cumple | `.gitignore` excluye builds, keystores, local.properties. `.editorconfig` presente. |
| 4 | **CI/CD** | ✅ Cumple | Pipeline completo: Secret Scan → SCA → SAST → Lint → Unit Tests → Debug APK → Release APK → AAB. **Mejora:** agregar tests instrumentados y Maestro E2E en nightly. |
| 5 | **Testing y QA** | ⚠️ Requiere ajuste | 5 test files: `AuthRepositoryTest`, `HeartRateParserTest`, `PreventiveRiskEngineTest`, `SecureStoresTest`, `SyncWorkersTest`. Smoke test Compose (`SmokeComposeTest`). Maestro flow definido (`patient_smoke.yaml`). **Falta:** ejecución de Maestro en CI, tests de ViewModels, tests de integración DAO completos. |
| 6 | **DevSecOps** | ✅ Cumple | Gitleaks, OWASP Dependency-Check con SARIF, ktlint, detekt, Android Lint. Pipeline sólido. |
| 7 | **Seguridad de Aplicación** | ✅ Cumple | `BiometricAuthManager` con `BiometricPrompt` + `CryptoObject` + Android KeyStore. `EncryptedSharedPreferences` (AES256). SQLCipher. Certificate Pinning (principal + refreshClient). `FLAG_SECURE` en `MainActivity`. Sentry al 20% con redacción. R8/ProGuard en release. `network_security_config.xml` con cleartext deshabilitado. |
| 8 | **Datos y BD** | ✅ Cumple | Room con SQLCipher. Schema exportado (`schemas/`). Migraciones no destructivas. Estado `SYNCED` en entidades. No hay datos mock en producción. |
| 9 | **Observabilidad y Monitoreo** | ⚠️ Requiere ajuste | Sentry integrado (rate 20%, redacción PHI). **Falta:** logs estructurados en producción, métricas de battery drain, crash-free rate dashboards, performance traces. |
| 10 | **Resiliencia, Backups y DR** | ✅ Cumple | Offline-First: Room persiste datos localmente. SyncEngine con outbox y retry exponencial. Halt Rollout documentado en Runbook para Google Play. |
| 11 | **Compliance y Auditoría Médica** | ✅ Cumple | BiometricGate en arranque. PHI cifrado en reposo (SQLCipher) y en tránsito (TLS + certificate pinning). Sentry con filtros de PHI/tokens. |
| 12 | **Operación, Incidentes y Mejora Continua** | ⚠️ Requiere ajuste | Runbook con protocolo de Halt Rollout en Play Console. **Falta:** Feature Flags para rollout gradual, Remote Config para toggles de funcionalidades. |

---

## 3. Plan de Nuevas Funcionalidades y Mejoras

### A. Sincronización y Telemetría Wearable

| Área | Estado Actual | Acción Requerida | Prioridad |
|:---|:---|:---|:---:|
| BLE Connection Manager | ✅ `BleConnectionManager.kt` | Extender parsers para métricas faltantes: EDA (electrodermal activity), temperatura cutánea, PTT (pulse transit time). | P1 |
| Heart Rate Parser | ✅ `HeartRateParser.kt` | Agregar extracción de intervalos R-R para cálculo de HRV (rMSSD, SDNN) directamente en la app. | P1 |
| TFLite on-device inference | ✅ `PreventiveRiskEngine.kt` con 5 modelos | Agregar modelos faltantes: estrés/fatiga SNC, detección de infecciones, riesgo hipertensión, VO2max. | P1 |
| Sync Worker multi-métrica | ✅ `NormalSyncWorker` con estado SYNCED | Implementar canal crítico: cuando SpO2 < 90% o HR > 180bpm, enviar inmediatamente al backend sin esperar batch. | P0 |
| Config de frecuencia/batch dinámico | ❌ No implementado | Consumir `GET /v1/devices/{id}/sync-config` del backend para ajustar `samplingIntervalMs` y `batchSize` dinámicamente. | P1 |
| Foreground Service BLE | ✅ `BleScanForegroundService.kt` | Verificar estabilidad en Android 14-15 con restricciones de exact alarms y background execution. | P2 |

### B. Panel de Configuración y Personalización

| Área | Estado Actual | Acción Requerida | Prioridad |
|:---|:---|:---|:---:|
| Tema claro/oscuro | ✅ `Theme.kt` con Material3 dynamic colors | Agregar toggle manual en Settings y persistir preferencia vía API. Soporte para `isSystemInDarkTheme()` + override manual. | P2 |
| Personalización visual | ⚠️ Parcial — colores definidos en `Color.kt` | Permitir selección de color de acento desde Settings. Persistir en DataStore + sincronizar con backend. | P3 |
| Preferencias de notificación | ❌ No implementado | Crear pantalla de configuración de notificaciones: canales (push, email), tipos de alerta (SOS, rutina, recordatorio medicación), horarios silenciosos. | P1 |
| Edición de datos de paciente | ❌ No hay pantalla dedicada | Crear `EditProfileScreen` Composable con formulario completo: datos personales, contacto emergencia, datos basales, foto de perfil. Conectar con API `PUT /v1/patients/me/health-profile`. | P1 |
| Edición de datos de cuidador | ❌ No implementado | Crear `EditCaregiverProfileScreen` para cuidadores. | P1 |
| Gestión de dispositivos | ❌ No hay UI | Crear pantalla de gestión de wearables vinculados: estado, batería, última sync, desvinculación. | P1 |

### C. Módulo de Historia Clínica Integral

| Sección | Estado Móvil | Acción Requerida | Prioridad |
|:---|:---|:---|:---:|
| 1. Perfil e Identificación | ⚠️ Datos básicos en `Models.kt` | Crear Composable de perfil expandido con campos opcionales: Rh, fecha nacimiento, contacto de emergencia. | P1 |
| 2. Alergias y Reacciones | ❌ No hay UI | Crear `AllergiesScreen` con lista editable + FAB para agregar. Campos: alérgeno, tipo, severidad, manifestaciones. | P1 |
| 3. Medicación y Tratamientos | ⚠️ Modelo `Medication` existe | Expandir vista con: vía, frecuencia detallada, widget de adherencia (%, calendario de tomas). Integrar con `MedicationLog`. | P1 |
| 4. Antecedentes Patológicos | ❌ No hay UI | Crear sección: enfermedades crónicas (ICD-10), cirugías, hospitalizaciones, implantes. Timeline visual con LazyColumn. | P1 |
| 5. Antecedentes Gineco-Obstétricos | ❌ No existe | Crear sección condicional con campos: menarquia, FUR, G-P-C-A, anticonceptivos, embarazo actual. | P2 |
| 6. Antecedentes Heredofamiliares | ❌ No existe | Crear formulario de antecedentes familiares por parentesco y condición. | P2 |
| 7. Estilo de Vida y Hábitos | ❌ No existe | Crear cuestionario digital: tabaquismo, alcohol, ejercicio, sueño. Con validaciones y opciones estandarizadas. | P2 |

### D. Inferencia ML On-Device para Estimaciones Biométricas

| Condición / Métrica | Modelo TFLite | Estado | Acción Requerida | Prioridad |
|:---|:---|:---:|:---|:---:|
| Picos de glucosa / Riesgo metabólico | `glucose_patterns.tflite` | ✅ Presente | Integrar en `PreventiveRiskEngine` con features: HR, HRV, movement_variance, temp_c. Umbral de alerta configurable. | P1 |
| Arritmias (FA, Taqui/Bradicardia) | `heart_rate_anomaly.tflite` | ✅ Presente | Ya integrado. Mejorar: agregar análisis de intervalos R-R para detección de FA. | P1 |
| Apnea del sueño / Hipoxemia | `spo2_critical.tflite` | ✅ Presente | Ya integrado. Mejorar: cruzar con datos de movimiento nocturno para cribado de apnea. | P1 |
| Calidad de sueño | `sleep_quality.tflite` | ✅ Presente | Ya integrado. Crear UI de reporte matutino con stages estimados. | P2 |
| Reconocimiento de actividad | `activity_recognition.tflite` | ✅ Presente | Ya integrado. Mejorar: contextualizar métricas cardíacas según actividad detectada. | P2 |
| **Estrés crónico / Fatiga SNC** | ❌ No existe | ❌ | Crear modelo con features: rMSSD, SDNN, EDA, temperatura. Exportar a TFLite e incluir en assets. | P1 |
| **Detección temprana infecciones** | ❌ No existe | ❌ | Crear modelo basado en elevación de temp basal nocturna + taquicardia en reposo. | P1 |
| **Riesgo hipertensión / Rigidez arterial** | ❌ No existe | ❌ | Requiere sensor PTT. Crear modelo basado en morfología PPG. | P2 |
| **VO2max / Riesgo cardiovascular global** | ❌ No existe | ❌ | Crear modelo con PPG + acelerómetro + GPS para recuperación cardíaca post-esfuerzo. | P2 |

---

## 4. Acciones Inmediatas (P0)

> [!IMPORTANT]
> Acciones prioritarias para el módulo móvil:

1. **Implementar canal crítico de sincronización** — Cuando SpO2 < 90% o HR > 180bpm, enviar bypass de batch al backend inmediatamente.
2. **Agregar tests de Maestro E2E en CI** — El flujo `patient_smoke.yaml` existe pero no se ejecuta en pipeline.
3. **Crear pantalla de edición de perfil** — No hay forma de editar datos del paciente desde la app.
4. **Implementar pantalla de preferencias de notificación** — Crítico para compliance y experiencia de usuario.
5. **Verificar compatibilidad Android 14-15** — Restricciones de foreground services y exact alarms pueden afectar BLE scanning.

---

## 5. Resumen Ejecutivo

| Categoría | Nota |
|:---|:---:|
| DevSecOps Pipeline | **A** (9/10) — Gitleaks, OWASP DC, ktlint, detekt, Android Lint |
| Seguridad de Aplicación | **A** (9.5/10) — Biometric, SQLCipher, Certificate Pinning, FLAG_SECURE |
| Testing y QA | **B-** (6.5/10) — Tests unitarios sólidos, falta E2E en CI |
| Arquitectura y Código | **A** (9/10) — Clean Architecture, Hilt DI, Offline-First |
| Observabilidad | **C** (5.5/10) — Sentry básico, sin métricas de battery/performance |
| Historia Clínica UI | **D** (3/10) — Sin pantallas de historia clínica estructurada |
| Telemetría Wearable | **B** (7/10) — BLE + TFLite sólidos, faltan métricas y canal crítico |
| Personalización | **D+** (4/10) — Tema existe, sin settings ni edición de perfil |
