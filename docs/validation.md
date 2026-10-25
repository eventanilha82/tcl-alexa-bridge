# Validação e limites da versão atual

**Português (Brasil)** | [English](en/validation.md) · [Início](../README.md)

Ambiente testado: TCL C6K BR/G15, Android 14, V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25. O [relatório da 0.4.0](qa-v0.4.0.md) identifica o APK final, as provas do novo assistente e os limites de cobertura.

O APK final preservou vínculo na atualização, renovou tokens nativamente e passou por três ciclos de renovação/pausa/retomada, limpeza HTTP/Rx sem erro reportado, rebind do executor, reconexão do socket e dois volumes com efeito real/ACK, restaurando o nível original. DEX/manifest do build público, assinatura, produção sem depuração e licença BSD foram conferidos.

A primeira configuração foi exercitada em instalação isolada: autorização local, coleta da identidade TCL real sem perfil importado, início sem credenciais, registro nativo, código, confirmação do vínculo, renovação, conexão e persistência. O reinício passou sem abrir a interface: serviço, conta, nuvem, executor e persistência recuperados, com os ajustes de energia mantidos. O ciclo automático de 60 segundos em standby passou: desligamento nativo com ACK, tela em espera, nuvem/executor ativos e despertar por UDP sem abrir a interface. Voz pelo Echo na 0.4.0 ainda não foi confirmada nesta rodada; a confirmação anterior é histórica. Instalador com SmartAlexa ausente, nova confirmação de skill numa conta inteiramente nova, restauração de energia pela interface e uma noite inteira em espera não foram exercitados nesta rodada; são limites da cobertura, não falhas observadas.

A evolução anterior da ponte teve resultados positivos de volume e mudo, HDMI1–4/TV, YouTube/Netflix, navegação, pausa/retomada, renovação, reconexão, rebind, boot sem abrir a interface e despertar. O usuário confirmou volume, desligar e ligar pelo Echo em teste anterior. Mantemos esse contexto como histórico de implementação, sem atribuí-lo automaticamente à versão atual.

## O que um retorno comprova

As 11 famílias de `msg_type` analisadas no executor são 3,4,5,6,7,8,9,11,15,16,17. A ponte encaminha o protocolo nativo; isso não garante que todo recurso exista em todo modelo ou aplicativo. As métricas contam encaminhamentos ao executor e seus retornos. Um ACK de sucesso não comprova sozinho o efeito observado no conteúdo, player ou periférico.

Em testes anteriores do player TCL, seek/retrocesso/avanço/reinício alteraram posição e Stop removeu a sessão. Pause apresentou telemetria inconsistente apesar de retorno nativo de sucesso. Próximo/anterior em playlist de um único item não comprovam troca. Não houve canal sintonizado para teste positivo nem gravação real. PVR e canais precisam de validação no aparelho com os recursos disponíveis.

Existe uma janela nativa de `BINDING` em que mensagens podem ser descartadas antes de chegar à ponte. Não fazemos reenvio indiscriminado, pois isso pode duplicar ações. Uma interrupção abrupta durante renovação também depende do estado do backend TCL.

## Como validar sua instalação

Registre modelo, firmware e versões dos três pacotes. Depois confira:

1. Preparação pelo controle remoto, código e vínculo sem importar credenciais.
2. Controle por voz usando o nome exato da TV, com a interface fechada.
3. Volume/mudo, entradas disponíveis e apps realmente instalados.
4. Reprodução, pausa, busca e navegação em cada player usado.
5. Perda/retorno de rede e recuperação do executor.
6. Pausa/retomada e renovação sem perder vínculo.
7. Reinício sem abrir o app, standby curto e uma noite em espera.
8. Desativação das opções de energia e restauração do estado anterior.

Separe o que foi visto na tela, o que foi executado por voz e o que é apenas telemetria. Sem teste positivo de hardware e frase correspondente, classifique como condicionado ou inconclusivo. Não publique tokens, IDs, MACs, perfis ou logs TCL brutos em issues.

## Diagnóstico resumido

Em um computador com acesso ADB autorizado na TV, execute na raiz do repositório:

```sh
mkdir -p .private
./scripts/diagnostics.sh IP_DA_TV:5555 > .private/diagnostics.txt
```

Esse resumo evita o logcat TCL bruto, que pode conter dados de autenticação. Revise antes de compartilhar. Informe modelo, região, firmware, versões dos pacotes, comando/frase usados e o efeito observado; omita identificadores privados e credenciais.
