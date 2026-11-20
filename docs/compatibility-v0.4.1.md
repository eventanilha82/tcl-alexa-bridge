# Compatibilidade do núcleo TCL: candidato 0.4.1-rc1

Data: 20/11/2026. A versão estável publicada continua sendo a 0.4.0. Este candidato ainda não foi instalado nem qualificado em uma TV.

## Problema e correção

O bridge 0.4.0 procura campos públicos e um construtor público no núcleo TCL instalado. No APK 10.65.12 examinado, vários desses campos e o construtor de `AlexaManager` são privados. A inicialização falha antes de conseguir registrar a TV.

A 0.4.1-rc1 centraliza a resolução em `CoreReflection`: ela procura campos declarados, inclusive em superclasses, e resolve o construtor exato, permitindo acesso aos membros privados selecionados. O ajuste cobre inicialização, estado e leitura de tokens, heartbeat, recuperação do executor e encerramento. Não modifica o APK TCL instalado, suas flags de acesso ou o protocolo de rede.

O preenchimento de `DeviceInfo` passa a falhar quando um campo solicitado não existe ou tem um tipo inesperado. A ausência do campo não é mais ignorada silenciosamente.

## Resultados locais

| Núcleo | Origem da amostra | Contrato de 51 verificações | Adaptador SDK | Busca pública antiga |
| --- | --- | --- | --- | --- |
| 10.58.16 | TV C6K/G15 e firmware G15 já disponível | Passou | 1 correspondência | Nenhum bloqueio nos membros verificados |
| 10.58.22 | Extraído do firmware R75PT01 V618 | Passou | 1 correspondência | Nenhum bloqueio nos membros verificados |
| 10.60.15 | Extraído do firmware T653T01 V655 | Passou | 1 correspondência | Nenhum bloqueio nos membros verificados |
| 10.60.17 | Extraído dos firmwares R75PT01 V645 e V648; APKs idênticos | Passou | 1 correspondência | Nenhum bloqueio nos membros verificados |
| 10.65.12 | Catálogo de atualizações TCL | Passou | 1 correspondência | 37 acessos selecionados seriam incompatíveis com busca exclusivamente pública |
| 10.60.03 | Amostra ainda não obtida | Pendente | Pendente | Pendente |

Esses resultados são de análise do DEX e testes locais do adaptador. Não comprovam que o fluxo completo de Alexa funciona em cada versão. A 10.60.17 de firmware mantém públicos os membros inspecionados; não podemos inferir a causa da falha relatada na 10.60.03 a partir dela.

O teste JVM de `CoreReflection` passou por 11 verificações: estado privado e público, campo herdado, estado estático e de instância, inteiro de 64 bits, membro ausente, assinatura de construtor incorreta e propagação da falha do construtor. O teste do adaptador SDK também confere o DEX modificado e rejeita cabeçalho inválido.

Os APKs extraídos foram conferidos por assinatura. O V618 tem SHA-256 `01c5d871c08f33329bab5715f10024ebb235cb1553618a3455edeed5ba47c1f5`; seu TCSCore 10.58.22 tem SHA-256 `d186f35fbd7023dd0a5ae78fbeb13987d562ec08b8162f8c50300c3b6209c820`. O núcleo 10.60.17 de V645/V648 tem SHA-256 `67251c81e370bdd294a485dddf1c9e4ffd39d570f862b423d61477617515feea`. O TCSCore 10.60.15 extraído de T653 V655 tem SHA-256 `a905fd3c99c8ec2d5c2dd0e4901e3401cf6f82398d384b3f64cefa5d4928faac`. Todos os núcleos examinados mantêm o mesmo certificado TCL nas amostras disponíveis.

## Reproduzir os testes locais

Com um JDK compatível com `javac --release 8` e Python 3:

```bash
bash scripts/test-core-compatibility.sh /caminho/TCSCore-base.apk /caminho/TCSCore-novo.apk
```

Sem argumentos, o script executa o teste JVM de reflexão. Com APKs locais, também confere os campos/tipos, construtores e métodos selecionados diretamente nos DEX e executa o teste do patch SDK. Os APKs TCL devem ser obtidos separadamente e não entram no repositório fonte.

## Probe Android em segundo plano

Foi preparado um teste de instrumentação com pacote separado, `local.tclbridge.compatprobe`, sem Activity, acesso à conta ou permissão `INTERNET`. Ele carrega cópias privadas dos núcleos fornecidos, usa o mesmo código de reflexão e adaptação do bridge e verifica objetos sintéticos. Não chama registro, renovação, gravação de credenciais nem comandos do SmartAlexa.

Após configurar o SDK como no guia de build e compilar o bridge uma vez:

```bash
bash scripts/build-core-probe.sh /caminho/TCSCore-base.apk /caminho/TCSCore-novo.apk
adb install -r build/core-probe/core-probe.apk
adb shell am instrument -w local.tclbridge.compatprobe/local.tclbridge.CoreCompatibilityProbe
```

A construção local do probe foi verificada. Sua execução na TV está pendente: o computador e a TV estavam em redes distintas e o teste foi adiado pelo usuário. O probe não faz parte do APK distribuído do bridge e não deve ser anexado à release pública, pois contém os APKs fornecidos para teste.

## Antes da release estável

1. Executar o probe Android com a base e o núcleo novo, sem interromper a reprodução da TV.
2. Instalar o candidato do bridge, assinado com a mesma chave da release anterior, preservando seus dados.
3. Verificar conexão, executor, estado persistido e renovação real.
4. Validar registro/código em instalação isolada e autorizada, sem substituir o vínculo funcional da TV de uso.
5. Quando a TV estiver livre, testar volume, desligar/ligar e recuperação após standby/reinício.
6. Obter a 10.60.03 para reproduzir o relato original. Até lá, não anunciar esse bug como resolvido.

Nenhuma atualização de firmware, instalação de núcleo TCL ou alteração na TV foi necessária para os testes locais. Os resultados anteriores da 0.4.0 continuam históricos e não foram transferidos para este candidato.
