# Validação e limites

Ambiente inicial validado: C6K BR / G15, Android 14, V8-0016T01-LF1V048, TTVSCore 10.58.16, SmartAlexa 6.13.25.

Na implementação da ponte foram observados: volume e mudo, HDMI1–4/TV, abertura YouTube/Netflix, navegação, registro de retornos nativos, retorno de canal inexistente como falha, renovação de token, reconexão de socket, queda/rebind do executor, boot sem abrir a interface e despertar por UDP. O usuário confirmou volume, desligar e ligar por Echo na etapa anterior. Em 25/10/2026, o APK exato da release foi instalado e passou pela regressão de renovação, pausa, reconexão, comando real e reinício. O usuário confirmou volume, desligar e ligar pelo Echo após um minuto em espera. Veja o [relatório da release 0.3.4](qa-v0.3.4.md). Esta versão continua experimental para outras combinações de TV/firmware.

As 11 famílias de `msg_type` do executor analisado são 3,4,5,6,7,8,9,11,15,16,17. Cobertura de despacho não é prova de todos os efeitos. No player TCL, seek/retrocesso/avanço/reinício alteraram a posição e Stop removeu a sessão; Pause apresentou telemetria de estado inconsistente, apesar do retorno nativo de sucesso. Próximo/anterior numa playlist de um único item não comprovam troca. Não houve canal sintonizado para teste positivo nem gravação real.

Para validar outra TV, registre: pacotes e versões, boot, app fechado, volume/mudo, entradas reais, mídia em cada player, rede indisponível/retorno, queda do executor, renovação durante pausa, standby curto e uma noite em espera. Nunca publique credenciais nem o perfil da TV.

Sem teste positivo de hardware e frase de voz correspondente, classifique o item como condicionado/inconclusivo. Não alegue 100% de paridade ou estabilidade prolongada só com ACKs.
