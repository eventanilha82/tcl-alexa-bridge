# Verificação da 0.4.1 — pré-release

**Português (Brasil)** | [English](en/qa-v0.4.1.md) · [Início](../README.md)

Publicação para testes de compatibilidade; a estável continua sendo a 0.4.0. APK `TCL-Alexa-Bridge-0.4.1.apk`, versionCode 11, **1221573 bytes**, SHA-256:

```text
06131e0619d8474413b87ceb33310a33d6861efe3719c74c36725c95a74bd235
```

O asset é o candidato efetivamente instalado nos testes registrados em 20/11/2026. O novo build do mesmo fonte tem todos os membros ZIP fora de `META-INF/` idênticos ao candidato, incluindo DEX, manifesto, recursos e licenças. A assinatura usa o mesmo certificado da 0.4.0, permitindo atualização sem apagar dados. Diferenças no contêiner/assinatura podem alterar o hash de um novo build. [Resumo verificável](qa-v0.4.1.json).

## Resultados

Ambiente funcional: TCL C6K/G15 BR, Android 14/API 34, TTVSCore 10.58.16 e SmartAlexa 6.13.25. Os aplicativos de sistema e o vínculo não foram substituídos.

| Verificação | Resultado |
| --- | --- |
| Atualização da ponte sem abrir sua interface | Vínculo, nuvem, executor e persistência preservados |
| Renovação nativa | Expiração avançou e o estado atualizado foi persistido |
| Recuperação após desconectar executor e socket | Ambos reconectaram |
| Volume pelo motor nativo e callback | Efeito real, dois retornos concluídos, zero falhas; volume original restaurado |
| Standby real de 60 segundos | Estado Asleep, nuvem/executor/vínculo/persistência mantidos |
| Despertar por UDP em teste separado | Awake após três broadcasts; nenhuma tecla ADB usada |
| Testes locais | 11 checks de reflexão e ordenação DEX dos 64 APKs passaram |
| Probe Android isolado | 48/64 passaram; 16 bloqueados na preparação |

O primeiro emissor de teste falhou no Mac com “sem rota para o host” ao enviar unicast e encerrou antes do broadcast. O broadcast posterior acordou a TV. Isso não equivale a um ciclo completo aprovado numa única execução. A tentativa de reconexão ADB durante a preparação da release expirou; não foi possível fechar essa regressão agora.

## Escopo e pendências

A correção detecta membros e estruturas; não usa uma lista de versões para escolher quais reparar. Os testes Java da coleção não certificam registro ou todos os comandos Alexa em cada APK, firmware, região ou TV. Consulte a [matriz completa](compatibility-v0.4.1.md).

A 10.60.03 exata não foi obtida. Permanecem pendentes voz pelo Echo nesta candidata, ciclo único completo de standby/despertar, nova conta e uma noite em espera. Primeira configuração e boot têm evidência histórica da 0.4.0, não foram repetidos na 0.4.1. Esses limites justificam a pré-release; não há afirmação de compatibilidade universal ou paridade de 100%.

## Atualizar e relatar

Instale o APK por cima da versão oficial anterior, sem desinstalar nem limpar dados. SmartAlexa e TTVSCore continuam necessários. Siga o [guia pela TV](setup.md). Uma compilação própria usa outra chave e não atualiza o APK oficial diretamente; preserve sua chave para atualizar suas próprias compilações.

Informe modelo, Android, firmware, versões do Core/SmartAlexa e etapa/efeito observado. Não anexe tokens, códigos de ativação, perfis, identificadores privados, chaves ou logs TCL brutos. Voltar para um APK de versionCode menor pode exigir procedimento específico; não apague o vínculo para forçar downgrade. Consulte [recuperação](rollback.md).
