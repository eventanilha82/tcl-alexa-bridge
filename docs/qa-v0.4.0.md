# Verificação da versão 0.4.0

**Português (Brasil)** | [English](en/qa-v0.4.0.md) · [Início](../README.md)

APK de produção: `TCL-Alexa-Bridge-0.4.0.apk`, **82462 bytes**, SHA256:

```text
ea4df262ad9a0c93f07fb64cb12e7365d6d566c68c784ee4520cae5d9d6ba2f2
```

Ambiente: TCL C6K BR/G15, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25. Os testes distinguem a primeira configuração em instalação isolada do exercício do APK final na instalação de produção. APKs temporários de laboratório não são distribuídos.

## APK final de produção

| Verificação | Resultado |
| --- | --- |
| Atualização preservando perfil e vínculo | Passou |
| Renovação pelo motor nativo TCL | Passou |
| Renovação seguida de pausa e retomada | Três ciclos passaram, preservando o estado atualizado |
| Encerramento dos recursos HTTP/Rx privados | Sem erro reportado na TV após os ciclos; espera/limpeza fora da UI |
| Desconexão do executor e rebind | Passou |
| Fechamento do socket e reconexão | Passou |
| Comando de volume real e ACK nativo | Dois efeitos observados, dois retornos concluídos, zero falhas; volume original restaurado |
| Estado após recuperação | Conta vinculada, nuvem e executor conectados, estado nativo persistido, sem autenticações pendentes |
| Build pelo script público | DEX e manifest iguais aos do APK final conferido |
| Assinatura e depuração | Assinatura conferida; `debuggable=false` |
| Licença adblib | Aviso BSD presente no APK |
| Reinício sem abrir a interface | Passou; `BOOT_COMPLETED` iniciou o serviço |
| Recuperação após reinício | Conta, nuvem, executor e persistência confirmados |
| Energia após reinício | Low Power Standby global em 0 e espera por rede TCL em 1 mantidos |
| Desligamento nativo SmartAlexa | ACK código 0 em 39 ms; tela Asleep/Dozing observada |
| Standby automático de 60 segundos | Nuvem e executor permaneceram ativos |
| Despertar após standby | Magic packet UDP 1025 levou a tela a Awake, sem abrir a interface |

O reinício e o ciclo automático de standby de 60 segundos passaram sem abrir a Activity. A confirmação de voz pelo Echo na 0.4.0 ainda não foi feita nesta rodada; a confirmação de voz anterior permanece evidência histórica. Os resultados acima já descrevem o funcionamento efetivamente exercitado do APK final; não se deve estendê-los a todos os recursos ou a outras TVs.

## Primeira configuração na instalação isolada

| Verificação | Evidência |
| --- | --- |
| Autorização local ADB pelo usuário | Sessão de preparação autorizada na própria TV |
| Coleta sem arquivo importado | Identidade nativa TCL real salva antes de qualquer token |
| Ausência de identidade inventada | Interface TCL ou diagnóstico nativo; nenhum fallback por Android ID |
| Início sem credenciais ou banco nativo | Registro iniciado com armazenamento de autenticação vazio |
| Registro, código e confirmação | Fluxo nativo SDK completo |
| Renovação, nuvem, executor e persistência | Confirmados após o vínculo inicial |
| Runtime privado e compatibilidade SDK | Sem `NoSuchFieldError` no bootstrap final testado |

O último bootstrap isolado com o runtime revisado concluiu o fluxo entre 01:48:38 e 01:48:48. Esses horários descrevem uma execução observada, não um prazo garantido.

As verificações locais do cliente ADB passaram por 54 checks, incluindo pacotes de fechamento antigos. A adaptação SDK modifica apenas o método esperado na cópia privada; no DEX real examinado foram 21 bytes, com checksums válidos.

## Limites da cobertura de QA

O download/instalador Android visível com SmartAlexa ausente, uma nova confirmação de skill pelo celular para uma conta inteiramente nova, a restauração de energia pela interface e uma noite inteira em standby ainda não foram exercitados nesta rodada. São limites da cobertura de teste; não são falhas observadas desses recursos.

Também não há teste positivo de canal sintonizado, gravação/PVR ou todos os players/periféricos. Testes de versões anteriores observaram volume/mudo, entradas, abertura de apps, navegação, boot e despertar por voz, mas o resultado de cada combinação atual deve ser avaliado pelo teste correspondente.

Veja [validação](validation.md) para interpretar ACKs e limites. Não se deve afirmar 100% de paridade ou estabilidade prolongada com base apenas em despacho de comandos ou estados de conexão.
