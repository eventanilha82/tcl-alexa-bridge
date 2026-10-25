# Verificação da release 0.3.4 em 25/10/2026

APK testado: `TCL-Alexa-Bridge-0.3.4.apk`, SHA256 `86f8535bacb54f06a8807d60e6b03327ea5800aa62f251c0c3fda271fdf8e15b`. O DEX coincide com o produzido pelo script público. O arquivo foi baixado novamente do GitHub e seu SHA256 conferiu.

Ambiente: TCL C6K BR/G15, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16, SmartAlexa 6.13.25. Sem root, acessibilidade ou listener de notificações do bridge.

| Verificação | Resultado |
|---|---|
| Instalação como atualização preservando vínculo | Passou |
| Assinador TTVSCore e verificação da assinatura | Passou |
| Perfil privado válido | Passou |
| Renovação normal e credenciais persistidas | Passou |
| Renovação + pausa imediata + retomada | Passou; aguarda resposta antes de criar outra sessão |
| Pausa definitiva e retomada pelo botão | Passou |
| Queda do executor e rebind | Passou; executor recuperado em cerca de 2 segundos |
| Queda de socket e reconexão | Passou; nuvem recuperada em cerca de 2 segundos |
| Volume pelo caminho nativo | Passou; volume real 14 → 15 e ACK código 0 |
| Echo sem abrir o app: volume, desligar, esperar 1 minuto e ligar | Usuário confirmou os três; ACK de volume por voz registrado |
| Reinício sem abrir a interface | Passou; serviço iniciou por BOOT_COMPLETED após atraso do agendador TCL |
| Estado após reinício | Nuvem, executor, vínculo e persistência recuperados; perfil válido |
| Standby após reinício | Ajuste autorizado reaplicou Low Power Standby 0; UDP 1025 escutando |

Os resultados se limitam ao aparelho e às condições acima. Retorno de sucesso do executor não prova todos os efeitos de mídia, PVR ou periféricos. Veja docs/validation.md para as limitações de cobertura.
