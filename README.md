# ShifLauncher

Launcher minimalista para Android, inspirado no Niagara. **100% gratuito e open-source**, sem anúncios e sem funções pagas.
Feito pensando no POCO X7 Pro (HyperOS 3), mas funciona em qualquer Android 8+.

## Funções

- **Tela inicial limpa**: relógio, data e (opcional) próximo evento da agenda.
- **Favoritos**: até 8 apps, perto do polegar.
- **Notificações na lista**: ponto e prévia da última notificação ao lado do app.
- **Gaveta de apps** (deslize para cima) com **alfabeto lateral em onda** para rolar rápido.
- **Busca**: digite e aperte "Ir" para abrir o primeiro resultado. Apps ocultos também aparecem na busca.
- **Renomear / ocultar / desinstalar / info do app**: toque e segure em qualquer app.
- **Fundo**: papel de parede ou preto (ótimo para AMOLED).
- **Gestos estilo HyperOS**:
  - deslizar para cima → todos os apps;
  - deslizar para baixo → aba de notificações (ou busca, configurável);
  - dois toques → bloquear a tela;
  - tocar e segurar no vazio → configurações.
- Interface em **português e inglês** (segue o idioma do sistema; dá pra trocar em Configurações → Idioma).

## Instalação

1. Abra a página **[Releases](../../releases)** do repositório pelo celular.
2. Baixe o `ShifLauncher.apk` da versão mais recente e instale (permita "instalar apps desconhecidos" para o navegador).
3. Abra o ShifLauncher → toque e segure na tela → **Definir como launcher padrão**.
4. Nas Configurações do ShifLauncher, ative:
   - **Acesso às notificações** (para mostrar notificações na lista);
   - **Serviço de acessibilidade** (para bloquear a tela com dois toques e abrir as notificações).

> No HyperOS, ative também o **Início automático** do ShifLauncher e deixe a economia de bateria em
> "Sem restrições" (Configurações → Apps → ShifLauncher), senão o sistema pode desligar o serviço de acessibilidade.

Cada novo build instala por cima do anterior, sem perder as configurações.

## ⚠️ Gestos do sistema no HyperOS

A Xiaomi desativa a navegação por gestos em tela cheia quando o launcher padrão não é o dela.
Isso acontece com **qualquer** launcher de terceiros (Niagara, Nova, este aqui...). Existem duas saídas:

### Opção 1: comando ADB (recomendado, só precisa fazer uma vez)

1. No celular: Configurações → Sobre o telefone → toque 7× em **Versão do SO** para liberar as opções de desenvolvedor.
2. Configurações → Configurações adicionais → **Opções do desenvolvedor** → ative **Depuração USB**
   (no HyperOS ative também **Depuração USB (configurações de segurança)**, que pede login na conta Xiaomi).
3. No Windows, baixe o [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
   e extraia (ex.: `C:\platform-tools`).
4. Conecte o celular no USB, aceite a autorização na tela do celular e, no PowerShell dentro da pasta:

   ```powershell
   .\adb devices
   .\adb shell settings put global force_fsg_nav_bar 1
   ```

5. Em Configurações → Tela → **Tela cheia** (navegação), escolha **Gestos**.

Para desfazer: `.\adb shell settings put global force_fsg_nav_bar 0`.

Observações: a animação de "voltar para o início" pode ficar um pouco diferente, e algumas atualizações
do HyperOS podem resetar a configuração (basta rodar o comando de novo).

### Gestos do ShifLauncher (necessário no HyperOS 3)

No HyperOS 3 o comando acima esconde os botões, mas os gestos não funcionam com launcher de terceiros.
Por isso o ShifLauncher recria os gestos: Configurações do ShifLauncher → **Navegação do sistema** →
**Gestos de navegação estilo HyperOS** (precisa do serviço de acessibilidade ativo).

- deslizar de baixo para cima → início
- deslizar para cima e segurar → recentes
- deslizar para os lados na borda de baixo → app anterior
- deslizar de qualquer lateral para dentro → voltar

### Opção 2: app de gestos

Apps como **Fluid Navigation Gestures** desenham áreas de gesto por cima da tela e funcionam sem PC,
mas precisam de permissão de acessibilidade e às vezes conflitam com apps em tela cheia.

## Desenvolvimento

- Kotlin + Jetpack Compose, `minSdk 26`, `targetSdk 35`.
- O GitHub Actions (`.github/workflows/build.yml`) compila o APK a cada push e publica em Releases.
- Build local: `./gradlew assembleRelease` (precisa do Android SDK).
- O keystore em `app/shiflauncher.keystore` é público de propósito (para atualizações instalarem por cima).
  Não use essa chave para publicar na Play Store.
