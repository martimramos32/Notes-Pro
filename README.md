# 📝 Notes Pro — XML Views

Uma aplicação Android nativa de gestão de notas pessoais, construída com **Kotlin**, **Java** e **Firebase**, com suporte a imagens, notas fixadas e lembretes com notificações locais.

---

## 📱 Sobre a Aplicação

**Notes Pro** permite ao utilizador criar, editar e eliminar notas de forma simples e rápida. As notas são sincronizadas em tempo real com a cloud através do **Firebase Firestore**, garantindo que os teus dados estão sempre disponíveis e seguros.

A autenticação é feita por email e password via **Firebase Authentication**, o que garante que cada utilizador só tem acesso às suas próprias notas.

---

## ✨ Funcionalidades

### Implementação Base
- **Autenticação de utilizador** — Registo e login com email e password (Firebase Auth)
- **Criar notas** — Adicionar notas com título e conteúdo
- **Editar notas** — Alterar o título e o conteúdo de uma nota existente
- **Eliminar notas** — Remover uma nota permanentemente do Firestore
- **Sincronização em tempo real** — A lista de notas atualiza-se automaticamente sem necessidade de recarregar (via `FirestoreRecyclerAdapter`)

### 🚀 Melhorias Adicionadas

#### 🖼️ Imagens nas Notas
- Associar uma **imagem opcional** a cada nota, escolhida a partir da **galeria** ou tirada com a **câmara**
- As imagens são comprimidas (JPEG 70%, máximo 800×800px) e convertidas para **Base64**, sendo guardadas diretamente no Firestore — **sem custos de Firebase Cloud Storage**
- **Pré-visualização** da imagem no ecrã de edição e **miniatura (thumbnail)** na lista principal
- Possibilidade de **remover a imagem** de uma nota sem apagar a nota

#### 📌 Notas Fixadas (Pin)
- Fixar qualquer nota para aparecer **sempre no topo da lista**, independentemente da data de criação
- O estado "fixado" é persistido no Firestore e sincronizado entre dispositivos
- Ícone de pin visível em cada nota fixada na lista principal
- Toggle simples com ícone no ecrã de edição (pin outline → pin preenchido)

#### 🔔 Lembretes com Notificações Locais
- Agendar um **lembrete** para qualquer nota com um DatePicker + TimePicker
- Na hora agendada, o Android dispara uma **notificação** com o título da nota
- Clicar na notificação abre **diretamente** o ecrã de edição com todos os dados da nota preenchidos
- **Reboot-safe**: os lembretes são re-agendados automaticamente após o dispositivo reiniciar (`BootReceiver`)
- Validação de datas no passado — só são aceites horas futuras
- Possibilidade de cancelar um lembrete existente
- Ícone de sino visível na lista para notas com lembrete ativo
- Completamente **gratuito** — usa apenas APIs nativas do Android (`AlarmManager`, `BroadcastReceiver`, `NotificationManager`)

---

## 🛠️ Stack Técnica

| Componente | Tecnologia |
|---|---|
| Linguagem | Kotlin + Java |
| UI | XML Views (Material Design 3) |
| Autenticação | Firebase Authentication |
| Base de dados | Firebase Cloud Firestore |
| Lista em tempo real | FirebaseUI `FirestoreRecyclerAdapter` |
| Imagens | Base64 + compressão JPEG nativa Android |
| Notificações | `AlarmManager` + `BroadcastReceiver` + `NotificationCompat` |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 35) |

---

## 📁 Estrutura do Projeto

```
app/src/main/
├── java/com/notes/notesproxmlviews/
│   ├── BootReceiver.kt          # Re-agenda lembretes após reboot
│   ├── CreateAccountActivity.kt # Ecrã de registo
│   ├── ImageUtils.kt            # Compressão e conversão Base64 de imagens
│   ├── LoginActivity.kt         # Ecrã de login
│   ├── MainActivity.kt          # Lista principal de notas
│   ├── Note.java                # Modelo de dados da nota
│   ├── NoteAdapter.kt           # Adapter FirestoreRecyclerAdapter
│   ├── NoteDetailsActivity.kt   # Ecrã de criação/edição de notas
│   ├── ReminderReceiver.kt      # Dispara notificação ao receber alarme
│   ├── ReminderScheduler.kt     # Agenda/cancela alarmes (AlarmManager)
│   ├── SplashActivity.kt        # Ecrã inicial (splash screen)
│   └── Utility.kt               # Métodos utilitários (Firestore path, etc.)
├── res/
│   ├── drawable/                # Ícones e backgrounds
│   ├── layout/                  # Layouts XML das Activities
│   ├── values/                  # Strings, cores, temas
│   └── xml/
│       ├── file_paths.xml       # Configuração do FileProvider (câmara)
│       └── ...
└── AndroidManifest.xml
```

---

## ⚙️ Instruções de Execução

### Pré-requisitos
- **Android Studio** (Hedgehog 2023.1.1 ou superior recomendado)
- **JDK 21** (incluído no Android Studio — JetBrains Runtime)
- Conta Google com um projeto **Firebase** configurado
- **Android SDK** com API Level 24 ou superior instalado

### 1. Configurar o Firebase

1. Acede à [consola do Firebase](https://console.firebase.google.com/) e cria (ou abre) o teu projeto.
2. Ativa o **Firebase Authentication** com o provider "Email/Password".
3. Ativa o **Cloud Firestore** em modo de produção.
4. Faz download do ficheiro `google-services.json` e coloca-o na pasta `app/`.
5. Na consola Firestore, vai a **Índices → Criar índice composto** com os seguintes parâmetros:
   - **Coleção:** `my_notes`
   - **Campo 1:** `pinOrder` (Decrescente)
   - **Campo 2:** `timestamp` (Decrescente)
   - **Âmbito:** Coleção

### 2. Clonar e Abrir o Projeto

```bash
# Clonar o repositório
git clone <URL_DO_REPOSITORIO>

# Abrir no Android Studio: File → Open → selecionar a pasta NotesProXMLViews3
```

### 3. Compilar e Executar

#### Via Android Studio
1. Aguarda o Gradle sincronizar as dependências.
2. Conecta um dispositivo Android via USB (com modo de depuração USB ativo) ou usa um Emulador (API 24+).
3. Clica no botão **▶ Run** (ou usa `Shift+F10`).

#### Via Linha de Comandos (Windows)
```powershell
# Importante: usar o JDK do Android Studio para evitar conflitos
$env:JAVA_HOME = "D:\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
O APK gerado estará em: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Permissões Necessárias

A aplicação pedirá as seguintes permissões em tempo de execução:

| Permissão | Propósito |
|---|---|
| `CAMERA` | Tirar fotografias para adicionar às notas |
| `READ_MEDIA_IMAGES` | Selecionar imagens da galeria (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Selecionar imagens da galeria (Android 12 e inferior) |
| `POST_NOTIFICATIONS` | Receber notificações de lembretes (Android 13+) |

---

## 📌 Notas Importantes

> **Índice Composto Firestore:** A funcionalidade de Notas Fixadas requer um índice composto no Firestore (ver passo 5 da configuração). Sem este índice, as notas não aparecem na lista. O índice demora cerca de 1-2 minutos a ser criado após submissão.

> **Limite de tamanho das imagens:** O Firestore tem um limite de 1 MiB por documento. As imagens são automaticamente comprimidas e redimensionadas para respeitar este limite.

> **AlarmManager — Android 12+:** Em dispositivos com Android 12 ou superior, a permissão `SCHEDULE_EXACT_ALARM` é necessária para que os lembretes sejam exatos. A app inclui esta permissão no Manifest.

---

## 👨‍💻 Desenvolvido com

- [Firebase](https://firebase.google.com/) — Autenticação e base de dados
- [FirebaseUI for Android](https://github.com/firebase/FirebaseUI-Android) — Sincronização em tempo real
- [Material Design Components](https://m3.material.io/) — Interface visual
