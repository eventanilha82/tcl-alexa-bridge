# Revisão técnica da versão atual

A revisão foi dividida entre runtime/Binder/ciclo de vida e onboarding/portabilidade/distribuição. Este documento descreve as correções presentes e seus limites; os resultados executados estão no [relatório da 0.4.0](qa-v0.4.0.md).

| Área | Correção ou proteção |
| --- | --- |
| Binder e morte do executor | Rastrear e desfazer o registro da ServiceConnection; recuperar o executor |
| Callbacks e pausa | Guardas de estado, encerramento serializado e drenagem de renovação antes de nova sessão |
| Conexão | Worker dedicado e proteção contra sobreposição |
| Assinatura TCL | Assinador instalado; não incorporar ou exportar material privado TCL |
| Identidade | Coleta nativa da própria TV; perfil privado e asset público vazio |
| Ações administrativas | Removidas da Activity exportada; diagnóstico protegido pelo sistema |
| Observadores e temporizadores | Instalação durante conexão Binder; cancelamento e guardas após stop |
| Energia | Opt-in, aviso de consumo e preservação de valores originais; rede TCL configurada pela sessão shell |
| Runtime privado | Hash da origem, validação ZIP/DEX e checksums; adaptação SDK restrita e propriedades Alexa isoladas |
| Preparação local | Somente loopback, comandos fixos e chave ADB privada temporária |
| Cliente ADB | Tratamento de pacotes CLSE antigos coberto pela suíte local |
| Instalador | HTTPS, limites de tamanho, hash/pacote/versão/certificado e provider de leitura com caminho único |
| Interface | Diagnósticos separados de componentes; reconhecimento de vínculo existente e estado recente |
| Build/distribuição | Dependências explícitas, perfil sem identidade no fonte, produção sem depuração e licença BSD incluída |

Não há root, serviço de acessibilidade ou concessão de INJECT_EVENTS. A cópia privada do runtime não substitui pacotes de sistema nem grava firmware. A autorização de configuração não implica uma sessão ADB permanente.

A API mínima é 34 e a combinação comprovada está documentada no README. Interfaces internas da TCL podem mudar; outra TV/firmware precisa de teste próprio. ACKs não demonstram todos os efeitos de mídia/PVR/periféricos, e o backend participa da recuperação de autenticação. O APK final passou por atualização preservando vínculo, renovação/pausa/retomada, limpeza HTTP/Rx, rebind, reconexão e volume real. O boot recuperou o serviço sem abrir a interface; o ciclo automático de standby de 60 segundos e o despertar UDP passaram. Voz da versão atual ainda não foi confirmada nesta rodada. Os demais limites de QA estão identificados no relatório atual, sem presumir falhas em fluxos ainda não exercitados.
