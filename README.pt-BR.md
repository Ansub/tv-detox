<div align="center">

# TV Detox

### Deixe sua Android TV (ou TV Box, Mi Box etc) antiga rápida de novo com Claude Code, Codex ou Antigravity: uma tela inicial minúscula e sem anúncios, mais o prompt para fazer a limpeza.

Claude Code, Codex ou Antigravity faz o trabalho. Você apenas pressiona OK no controle remoto.

[![Release](https://img.shields.io/github/v/release/Ansub/tv-detox?color=black)](https://github.com/Ansub/tv-detox/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-black)](LICENSE)
[![YouTube](https://img.shields.io/badge/YouTube-@ansubkhan-FF0000?logo=youtube&logoColor=white)](https://www.youtube.com/@ansubkhan)

<img src="docs/home-screen.png" alt="Tela inicial do TV Detox" width="100%">

</div>

<table>
<tr>
<td width="33%" valign="top"><b>Remove o lixo</b><br>Bloatware, anúncios e linhas patrocinadas removidos.</td>
<td width="33%" valign="top"><b>Uma tela inicial calma</b><br>Seus aplicativos, suas entradas HDMI e um relógio. Nada mais.</td>
<td width="33%" valign="top"><b>Desfaça a qualquer momento</b><br>Os apps são desativados, nunca excluídos.</td>
</tr>
</table>

## Assista em funcionamento

<a href="https://www.youtube.com/watch?v=fP2SGGVsaB0"><img src="https://img.youtube.com/vi/fP2SGGVsaB0/maxresdefault.jpg" alt="Assista: Claude Made My Old Android TV Feel Brand New" width="100%"></a>

**[▶ Claude Made My Old Android TV Feel Brand New](https://www.youtube.com/watch?v=fP2SGGVsaB0)** · a configuração completa em uma TV real, em 4 minutos.

## Comece agora

**1. Ative o modo de desenvolvedor na TV.**
Configurações → Preferências do dispositivo (ou Sistema) → Sobre → pressione **Número da versão** 7 vezes. Em seguida, ative a **Depuração USB** nas **Opções do desenvolvedor**. [Precisa de ajuda?](#ajuda)

**2. Abra o Claude Code, Codex ou Antigravity no seu computador.**
O computador e a TV devem estar na mesma rede Wi-Fi.

**3. Cole este texto.**

```text
Ajude-me a limpar minha Android TV e deixá-la mais rápida.
Minha TV é uma <coloque a marca e modelo, ex: Xiaomi Mi TV Stick, TCL, etc.>.

1. Conecte-se à minha TV via Wi-Fi. Guie-me em qualquer confirmação que eu precise tocar na TV.
2. Antes de alterar qualquer coisa, liste todos os aplicativos em português claro:
   seguros para desativar, necessários para a TV funcionar e apps que eu uso.
   Verifique a memória RAM livre antes de começar.
3. Peça minha confirmação antes de cada alteração. Apenas desative os aplicativos em vez de apagá-los. Mantenha uma lista do que foi alterado e como desfazer.
4. NUNCA desative: Google Play Services (com.google.android.gms), Google Play Store, Configurações, Interface do Sistema (System UI), apps de configuração inicial, teclado (Gboard), Bluetooth, entradas HDMI, o sintonizador de TV aberta e antena (DTV), ou qualquer item que a TV precise para ligar. Na dúvida, mantenha ativado. A única exceção: se o instalador oferecer desativar o app de setup do Google TV porque ele está sequestrando o botão Home, pergunte-me primeiro.
5. Instale a tela inicial: baixe https://github.com/ChrysthianChrisley/tv-detox e execute o install.sh com o IP da minha TV e os meus apps de streaming favoritos (YouTube, Netflix, Globoplay, Prime Video, Disney+, etc.).
6. Desative as animações do sistema, o protetor de tela pesado, e configure a TV para suspender após 5 minutos de inatividade.
7. Aguarde 30 segundos após a última alteração para o Android salvar o estado, reinicie a TV uma vez e confirme se ela ligou normalmente antes de finalizar. Mostre-me a memória RAM livre antes e depois.
```

Pronto. O assistente perguntará antes de cada alteração.

> [!NOTE]
> Este procedimento funcionou para os autores, mas não constitui conselho profissional. Use por sua conta e risco. Você é responsável por sua própria TV.

## Ajuda

<details>
<summary><b>Como ativar o modo de desenvolvedor?</b></summary>
<br>

1. Na TV, abra **Configurações**, depois **Preferências do dispositivo** (ou **Sistema**), depois **Sobre**.
2. Pressione OK em **Número da versão** (ou **Compilação / Build**) 7 vezes até ver a mensagem "Você agora é um desenvolvedor".
3. Volte uma tela, abra **Opções do desenvolvedor** e ative **Depuração USB**. Se houver a opção **Depuração de rede** ou **Depuração sem fio**, ative-a também.
4. Na primeira vez em que seu computador se conectar, a TV perguntará "Permitir depuração USB?". Marque "Sempre permitir" e selecione **Permitir**.

Os menus podem variar um pouco por fabricante. Se tiver dúvidas, pergunte ao Claude, Codex ou Antigravity: *"Tenho uma TV \<marca e modelo\>. Como você pode se conectar a ela pelo meu computador?"*

</details>

<details>
<summary><b>Em quais TVs funciona?</b></summary>
<br>

Android TV 9, 11 e 12, e Google TV 14. Todas as alterações são testadas em emuladores e aparelhos reais.

</details>

<details>
<summary><b>Posso fazer isso em um Fire TV, Roku, Samsung ou LG?</b></summary>
<br>

- **Amazon Fire TV:** parcialmente. O Fire TV roda Android por baixo, então a limpeza de apps funciona. A Amazon bloqueia a alteração da tela inicial padrão nos aparelhos atuais, portanto a tela da Amazon é mantida.
- **Roku:** não. O Roku utiliza sistema operacional próprio, não Android, sem suporte a conexões de depuração para desativação de apps.
- **Samsung (Tizen):** não, pelo mesmo motivo.
- **LG (webOS):** não. Você ainda pode reduzir anúncios desativando as opções de recomendação e promoção nas configurações da TV.

Se sua TV ou TV box roda Android TV ou Google TV, o procedimento funciona. Isso inclui modelos da Sony, TCL, Semp, Xiaomi, Philips, Philco, AOC, Chromecast com Google TV e Nvidia Shield.

</details>

<details>
<summary><b>E em projetores ou celulares Android?</b></summary>
<br>

- **Projetores com Android:** geralmente sim, desde que possuam Opções do desenvolvedor e Depuração USB.
- **Celulares Android:** apenas a limpeza de aplicativos. A tela inicial foi desenhada exclusivamente para navegação por controle remoto de TV.

</details>

<details>
<summary><b>Algum cuidado no Chromecast com Google TV?</b></summary>
<br>

Ele roda o mesmo Google TV testado no projeto. Dois detalhes observados:
- O botão dedicado de YouTube do controle remoto pode ser afetado se o launcher original for desativado. Abra o YouTube pelo bloco da tela inicial.
- O Google TV pode tentar reativar sua própria tela inicial após atualizações do sistema. Se ocorrer, execute o script novamente.

</details>

<details>
<summary><b>O botão Home abre a tela de configuração do Google TV (Sony BRAVIA e outros)</b></summary>
<br>

Em algumas TVs com Google TV, após o launcher original (`com.google.android.apps.tv.launcherx`) ser desativado, o Android direciona o botão Home para o aplicativo de configuração (`com.google.android.tungsten.setupwraith`). O script `install.sh` detecta essa situação e pergunta se você deseja desativá-lo. `undo.sh` o reativa caso necessário.

Para reativar manualmente:

```sh
adb shell pm enable com.google.android.tungsten.setupwraith
adb shell pm enable com.google.android.apps.tv.launcherx
```

</details>

<details>
<summary><b>O assistente de IA diz que não consegue alcançar minha TV</b></summary>
<br>

A interface de chat via navegador roda em servidores remotos, não no seu computador local, e por isso não tem acesso aos dispositivos da sua rede Wi-Fi. É necessário utilizar uma ferramenta que execute localmente no terminal: Claude Code, Codex ou Antigravity.

Caso prefira não usar IA, utilize os scripts diretos (`install.sh` no Linux/Mac ou `iniciar.bat` no Windows).

</details>

<details>
<summary><b>Posso instalar a tela inicial sem IA?</b></summary>
<br>

Sim. Você só precisa do `adb`.

- **No Windows:** dê dois cliques em `iniciar.bat` (ou execute `install.bat` / `install.ps1`). O script baixa o ADB oficial automaticamente e conduz a instalação.
- **No Mac:** instale com `brew install android-platform-tools`.
- **No Linux:** instale o pacote `adb` da sua distribuição.

Em seguida, no terminal:

```sh
sh install.sh <IP_DA_TV>
sh install.sh <IP_DA_TV> "com.netflix.ninja,com.google.android.youtube.tv,com.globo.globotv"
sh install.sh <IP_DA_TV> "" "bluetooth,sound,picture"
```

Para desfazer:
- **No Windows:** dê dois cliques em `desfazer.bat` (ou execute `undo.bat`).
- **No Mac/Linux:** execute `sh undo.sh <IP_DA_TV>`.

</details>

<details>
<summary><b>Como alterar os aplicativos na tela inicial?</b></summary>
<br>

Pelo próprio controle remoto da TV. Segure o botão **OK** sobre um aplicativo (ou pressione a tecla **Menu**) para:

- **Mover:** use as setas para deslizar o app até a nova posição e pressione **OK**.
- **Substituir:** escolha outro aplicativo instalado para ocupar aquele espaço.
- **Remover do início:** remove o atalho da tela inicial. O app continua instalado na TV.
- **Desinstalar:** disponível para aplicativos que você mesmo instalou.
- **Informações do app:** para aplicativos de fábrica do sistema.

Para adicionar novos aplicativos, selecione o bloco **Adicionar app** na linha inferior.

</details>

<details>
<summary><b>Minha antena de TV aberta continuará funcionando?</b></summary>
<br>

Sim. As entradas HDMI, sintonizadores digitais de TV aberta (DTV) e componentes de hardware são expressamente preservados e mantidos intactos.

</details>

<details>
<summary><b>Como desfazer tudo?</b></summary>
<br>

- **No Windows:** execute `desfazer.bat`.
- **No Mac/Linux:** execute `sh undo.sh <IP_DA_TV>`.

O script reativa todos os aplicativos registrados em `/data/local/tmp/tvhome-disabled.txt` e desinstala o TV Detox, retornando à tela inicial de fábrica.

</details>

<details>
<summary><b>Minha TV ficou reiniciando em loop</b></summary>
<br>

O computador frequentemente consegue se conectar via ADB por alguns segundos durante o início do boot. Use esse intervalo para reativar os pacotes:

```sh
adb connect <IP_DA_TV>
adb wait-for-device
for p in $(adb shell pm list packages -d | tr -d '\r' | sed 's/package://'); do adb shell pm enable "$p"; done
```

Para remover apenas a tela inicial, execute `sh undo.sh <IP_DA_TV>`. Se o computador não conectar, utilize o modo de recuperação (recovery) ou restauração de fábrica da sua TV.

</details>

<details>
<summary><b>Para desenvolvedores: compile você mesmo</b></summary>
<br>

1. Defina seus aplicativos padrão em `apps.txt` (ou utilize a lista brasileira em `apps.pt-BR.txt`).
2. Instale o Java 17 e o Android SDK.
3. Execute `sh build.sh` e depois `sh install.sh <IP_DA_TV>`.
4. Execute `sh ci/smoke-test.sh <IP_DA_TV>` para validação de testes.

O projeto consiste em uma única tela em Java puro, sem bibliotecas pesadas nem Gradle, consumindo cerca de 30 MB de memória RAM.

</details>

## Contribuindo

Pull requests são bem-vindos para correções que mantenham o funcionamento em todas as marcas de TV. O projeto utiliza apenas aplicativos oficiais e configurações nativas do Android.

---

<div align="center">

Criado por **Ansub Khan**. Adaptado para o público brasileiro.

<sub>Licença MIT</sub>

</div>
