<div align="center">

# TV Detox (Brasil) 🇧🇷

### Deixe sua Android TV (ou TV Box, Mi Box etc) antiga rápida de novo com Claude Code, Codex ou Antigravity: uma tela inicial minúscula e sem anúncios, mais o prompt para fazer a limpeza.

[![Release](https://img.shields.io/github/v/release/Ansub/tv-detox?color=black)](https://github.com/Ansub/tv-detox/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-black)](LICENSE)

<img src="docs/home-screen.png" alt="Tela inicial do TV Detox" width="100%">

</div>

<table>
<tr>
<td width="33%" valign="top"><b>🧹 Remove o lixo</b><br>Bloatware, propagandas, rastreadores e canais patrocinados desativados.</td>
<td width="33%" valign="top"><b>📺 Tela inicial limpa</b><br>Seus aplicativos favoritos, entradas HDMI e o relógio. Nada além disso.</td>
<td width="33%" valign="top"><b>↩️ Desfaça a qualquer momento</b><br>Os apps são apenas desativados, nunca apagados. 100% reversível.</td>
</tr>
</table>

---

## ⚡ Começando em 3 Passos

### 1. Ative a Depuração na sua TV
1. Na TV, abra **Configurações** ⚙️ → **Preferências do dispositivo** (ou **Sistema**) → **Sobre**.
2. Desça até **Número da versão** (ou **Compilação / Build**) e aperte o botão **OK** do controle **7 vezes seguidas** até aparecer *"Você agora é um desenvolvedor"*.
3. Volte uma tela, entre em **Opções do desenvolvedor** e ative **Depuração USB** (e **Depuração de rede/sem fio**, se houver).
4. Verifique o endereço IP da TV em **Configurações → Rede e Internet** (ou **Status**).

### 2. No seu computador (na mesma rede Wi-Fi da TV)
Abra a sua ferramenta de IA favorita (**Antigravity**, [Claude Code](https://claude.com/claude-code), Codex ou Gemini).

### 3. Cole este Prompt

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

---

## 💻 Para quem usa Windows (Instalação Direta sem IA)

Se você preferir instalar diretamente pelo Windows sem usar ferramentas de IA:

1. Dê **dois cliques** no arquivo **`iniciar.bat`**.
2. Digite o IP da sua TV quando solicitado. O script baixa o ADB automaticamente, conecta e faz a configuração completa!

Para reverter ao estado original a qualquer momento: basta dar dois cliques em **`desfazer.bat`**.

---

## 📱 Aplicativos Populares no Brasil (`apps.txt`)

O arquivo [`apps.txt`](apps.txt) já vem configurado com os serviços de streaming e utilitários mais usados no Brasil:
* YouTube (`com.google.android.youtube.tv`)
* Netflix (`com.netflix.ninja`)
* Prime Video (`com.amazon.amazonvideo.livingroom`)
* Globoplay (`com.globo.globotv`)
* Disney+ (`com.disney.disneyplus`)
* Max (`com.wbd.stream`)
* Crunchyroll (`com.crunchyroll.crunchyroid`)
* Pluto TV (`tv.pluto.android`)
* Claro tv+ (`br.com.claro.clarotv`)
* Vivo Play (`com.telefonica.brasil.vivoplay.tv`)
* Toku+ (`com.tv.tokuplus`)
* Spotify e Deezer

> *Nota: Os aplicativos que você não tiver instalados na sua TV são simplesmente ignorados pela tela inicial.*

---

## ❓ Dúvidas Frequentes

<details>
<summary><b>Funciona em quais marcas de TV no Brasil?</b></summary>
<br>

Funciona em qualquer aparelho com **Android TV** ou **Google TV**:
- **Xiaomi:** Mi TV Stick, Mi Box S, TV Stick 4K
- **TCL / Semp:** Modelos com Android TV ou Google TV
- **Chromecast:** Chromecast com Google TV (HD e 4K)
- **Philips, Philco, AOC, Multilaser:** Modelos que rodam Android TV
- **Fire TV Stick (Amazon):** É possível fazer a limpeza de bloatware e ganho de memória, mas a Amazon bloqueia a troca da tela inicial padrão.

*(Não funciona em TVs Samsung com Tizen, nem LG com webOS ou aparelhos Roku, pois utilizam sistemas proprietários que não aceitam ADB).*
</details>

<details>
<summary><b>Como reverter tudo se eu não gostar?</b></summary>
<br>

Todos os aplicativos desativados são salvos em uma lista interna na própria TV. Para restaurar:
* **No Linux/Mac:** `sh undo.sh <IP_DA_TV>`
* **No Windows:** `.\desfazer.ps1 -IpDaTv <IP_DA_TV>`
* Ou via comando direto: `adb shell pm enable <nome-do-pacote>`
</details>

<details>
<summary><b>Minha antena de TV aberta continuará funcionando?</b></summary>
<br>

**Sim!** O guia e os scripts protegem expressamente os drivers do processador e sintonizadores de TV (`com.android.providers.tv`, `*.tvinput*`, etc.), garantindo que seus canais abertos e entradas HDMI continuem intactos.
</details>

---

## 📄 Licença

Distribuído sob a licença [MIT](LICENSE). Baseado no projeto original [Ansub/tv-detox](https://github.com/Ansub/tv-detox).
