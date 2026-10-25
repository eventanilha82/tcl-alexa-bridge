# Revisão com dois agentes

A revisão foi dividida em runtime/Binder/ciclo de vida e portabilidade/build/publicação. O responsável integrou as correções e executa a regressão na TV.

| Prioridade | Achado | Tratamento |
|---|---|---|
| P1 | Vínculo Binder antigo preservado após morte do executor | rastrear e desfazer o registro real da ServiceConnection |
| P1 | Callback HTTP tardio após liberar handler/estado | guard do fluxo nativo e encerramento serializado; pausa precisa drenar renovação |
| P1 | connectBlocking no looper principal | worker dedicado, sem sobreposição |
| P1 | Material de assinatura incorporado no APK | assinador do TTVSCore instalado; build sem classe/material gerado |
| P1 | Perfil individual no artefato | migração ao armazenamento privado e asset vazio na distribuição |
| P1 | Ações administrativas na Activity exportada | retiradas; QA sob DUMP/proteção do sistema |
| P2 | Observador instalado depois do primeiro comando | instalar durante conexão Binder |
| P2 | Temporizadores/métricas após stop | cancelar timers e proteger entregas tardias |
| P2 | API mínima declarada indevidamente | limitar a API 34 testada |
| P2 | Mudança global de energia por padrão | opt-in e restauração do valor anterior |
| P2 | Cache por tamanho/data sem integridade | hash de origem e validação ZIP/Dex |
| P2 | Build e perfil dependentes da máquina pessoal | scripts parametrizados e perfil local validado |

Limitações residuais: janela BINDING nativa; compatibilidade de reflexão limitada às versões testadas; resultado do player/periférico pode divergir de ACK; encerramento abrupto do processo durante refresh depende também do comportamento do backend. Não há root ou permissões de injeção de eventos adicionados ao app.
