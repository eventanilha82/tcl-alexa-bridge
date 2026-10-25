package local.tclbridge;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Remote-first setup. Sensitive state stays in the app's private storage. */
public class SetupActivity extends Activity {
  final Handler ui=new Handler(Looper.getMainLooper());final ExecutorService worker=Executors.newSingleThreadExecutor();
  LinearLayout body,steps;TextView progress,code,status;Button primary;CheckBox standby;
  int step,preparationEpoch,pageEpoch;boolean busy,destroyed,requestingCode;long requestStarted;volatile LocalAdb session;
  SmartAlexaInstaller installer;boolean resumed;
  final Runnable refresh=new Runnable(){public void run(){if(step==2)refreshLink();ui.postDelayed(this,1000);}};
  int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(8));return t;}
  LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.WHITE);StateListDrawable bg=new StateListDrawable();bg.addState(new int[]{android.R.attr.state_focused},new ColorDrawable(Color.rgb(58,106,138)));bg.addState(new int[]{android.R.attr.state_pressed},new ColorDrawable(Color.rgb(58,106,138)));bg.addState(new int[]{},new ColorDrawable(Color.rgb(40,54,70)));b.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(10);body.addView(b,p);b.setOnClickListener(v->action.run());return b;}
  @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getExternalFilesDir(null);step=saved==null?(!getSharedPreferences("bridge",0).getBoolean("setup_complete",false)&&getSharedPreferences("bridge",0).getInt("setup_permissions_version",0)==1&&TvPreparation.hasProfile(this)?2:0):saved.getInt("step",0);showStep(step);}
  void showStep(int next){step=next;pageEpoch++;
    LinearLayout root=new LinearLayout(this);root.setPadding(dp(40),dp(28),dp(40),dp(24));root.setBackgroundColor(Color.rgb(18,22,28));
    steps=column();steps.setPadding(0,0,dp(30),0);root.addView(steps,new LinearLayout.LayoutParams(0,-1,.29f));
    TextView brand=text("Alexa na TV",24,Color.WHITE);brand.setTypeface(null,Typeface.BOLD);steps.addView(brand);steps.addView(text("Configuração guiada",13,Color.LTGRAY));
    String[] labels={"1  Começar","2  Preparar a TV","3  Vincular Alexa","4  Tudo pronto"};for(int i=0;i<labels.length;i++){TextView item=text(labels[i],16,i==step?Color.rgb(150,218,246):Color.rgb(130,142,158));item.setPadding(0,dp(15),0,dp(15));steps.addView(item);}
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(0,-1,.71f));body=column();body.setPadding(dp(24),0,dp(6),dp(8));scroll.addView(body);setContentView(root);
    if(step==0)welcome();if(step==1)prepare();if(step==2)link();if(step==3)done();
    if(primary!=null)primary.requestFocus();
  }
  void title(String value,String detail){TextView t=text(value,27,Color.WHITE);t.setTypeface(null,Typeface.BOLD);body.addView(t);body.addView(text(detail,15,Color.rgb(185,198,214)));}
  void welcome(){title("Sua TV, conectada à Alexa","Vamos preparar esta TV e vincular sua conta. Use o controle remoto; o celular será necessário para concluir a etapa da Alexa.");
    progress=text("Verificando os componentes TCL…",15,Color.LTGRAY);body.addView(progress);
    primary=button("Verificando…",()->showStep(1));primary.setEnabled(false);
    final int page=pageEpoch;worker.execute(()->{JSONObject components=TvPreparation.components(this);boolean nativeOk=components.optBoolean("ready");ui.post(()->{if(destroyed||step!=0||page!=pageEpoch)return;progress.setText(components.optString("message"));boolean prepared=getSharedPreferences("bridge",0).getInt("setup_permissions_version",0)==1&&TvPreparation.hasProfile(this);primary.setText(nativeOk?(prepared?"Continuar configuração":"Começar configuração"):"Verificar novamente");primary.setEnabled(true);primary.setOnClickListener(v->{if(nativeOk)showStep(prepared?2:1);else showStep(0);});if(components.optBoolean("can_install_smartalexa"))button("Instalar SmartAlexa",()->{if(installer!=null)installer.close();installer=new SmartAlexaInstaller(this);installer.begin();});});});
    button("Sair por enquanto",()->finish());
  }
  void prepare(){title("Prepare esta TV","A autorização do Android permite configurar o início automático, as notificações e o funcionamento em segundo plano. A conexão de configuração é encerrada ao terminar.");
    body.addView(text("1. Abra as opções de desenvolvedor e ative a depuração por rede.\n2. Volte para esta tela e selecione Autorizar e preparar.\n3. Quando o Android pedir, selecione Permitir.",15,Color.WHITE));
    button("Abrir configurações da TV",()->openDeveloperSettings());
    button("Como habilitar as opções de desenvolvedor",()->new AlertDialog.Builder(this).setTitle("Preparar a TV pelo controle").setMessage("Em Configurações → Sistema → Sobre, selecione a versão/compilação do Android TV sete vezes. Volte a Sistema, abra Opções do desenvolvedor e ative Depuração USB/por rede, conforme o menu da TCL. Volte ao app. Nenhum comando ou computador é necessário.\n\nSe esta TV não oferecer ADB pela rede na porta 5555, a preparação local não estará disponível neste firmware.").setPositiveButton("Entendi",null).show());
    standby=new CheckBox(this);standby.setText("Permitir ligar a TV por Alexa em espera");standby.setTextColor(Color.WHITE);standby.setTextSize(16);standby.setChecked(PowerCompatibility.enabled(this));body.addView(standby);
    body.addView(text("Essa opção usa uma permissão de configurações protegidas para manter a conexão com a tela apagada. Pode aumentar o consumo em espera; você poderá restaurar a configuração depois.",13,Color.rgb(180,192,208)));
    progress=text("",15,Color.rgb(150,218,246));progress.setVisibility(View.GONE);body.addView(progress);
    primary=button("Autorizar e preparar",()->runPreparation());button("Voltar",()->{if(busy)cancelPreparation();else showStep(0);});
  }
  void runPreparation(){if(busy)return;busy=true;progress.setVisibility(View.VISIBLE);primary.setEnabled(false);standby.setEnabled(false);final int attempt=++preparationEpoch;final boolean wake=standby.isChecked();final LocalAdb adb=new LocalAdb();session=adb;
    worker.execute(()->{try{TvPreparation.run(this,adb,wake,true,message->ui.post(()->{if(!destroyed&&step==1&&attempt==preparationEpoch)progress.setText(message);}));ui.post(()->{if(!destroyed&&attempt==preparationEpoch){busy=false;showStep(2);}});}
      catch(Exception e){ui.post(()->{if(!destroyed&&step==1&&attempt==preparationEpoch){busy=false;primary.setEnabled(true);standby.setEnabled(true);progress.setText(e instanceof java.net.ConnectException?"Ative a depuração por rede nas configurações da TV e tente novamente.":e instanceof java.net.SocketTimeoutException?"A autorização não foi concluída. Tente novamente e confirme a mensagem do Android.":e.getMessage()==null?"Não foi possível preparar a TV. Tente novamente.":e.getMessage());}});}
      finally{adb.close();if(session==adb)session=null;}
    });
  }
  void cancelPreparation(){preparationEpoch++;LocalAdb active=session;if(active!=null)active.close();busy=false;primary.setEnabled(true);standby.setEnabled(true);progress.setText("Preparação interrompida. Etapas já autorizadas podem ter sido aplicadas. Seu vínculo está preservado.");}
  void link(){title("Vincule sua conta Alexa","No celular, abra o aplicativo Alexa, encontre a skill TCL Assistant e siga o fluxo de vínculo indicado por ela. Informe o código desta TV quando solicitado.");
    code=text("",32,Color.WHITE);code.setTypeface(null,Typeface.BOLD);body.addView(code);status=text("",15,Color.LTGRAY);body.addView(status);
    primary=button("Gerar meu código",()->generateCode());button("Revisar preparação",()->showStep(1));
    if(!state().optBoolean("linked"))try{JSONObject prior=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(getFilesDir(),"link-code.json").toPath()),StandardCharsets.UTF_8));if(prior.optLong("expires_at")>System.currentTimeMillis()/1000)startControl("RESUME_LINK");}catch(Exception ignored){}
    refreshLink();
  }
  JSONObject state(){try{return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(getFilesDir(),"state.json").toPath()),StandardCharsets.UTF_8));}catch(Exception e){return new JSONObject();}}
  void generateCode(){if(requestingCode)return;requestingCode=true;requestStarted=SystemClock.elapsedRealtime();code.setText("");primary.setEnabled(false);status.setText("Gerando seu código na TCL…");startControl("LINK");}
  void startControl(String action){getSharedPreferences("bridge",0).edit().putBoolean("enabled",true).apply();startForegroundService(new Intent(this,BridgeService.class).setAction(action));}
  void refreshLink(){if(step!=2)return;JSONObject s=state();
    if(s.optBoolean("linked")){requestingCode=false;code.setText("Conta vinculada");long age=System.currentTimeMillis()-s.optLong("updated_at");boolean ready=age>=0&&age<5000&&s.optBoolean("service_running")&&s.optBoolean("enabled")&&s.optBoolean("connected")&&s.optBoolean("executor")&&s.optBoolean("persisted");status.setText(ready?"Conexão e controles confirmados. Você já pode usar seu Echo.":"Seu vínculo está salvo. Preparando a conexão e os controles TCL…");primary.setEnabled(true);primary.setText(ready?"Concluir configuração":"Ativar controle");primary.setOnClickListener(v->{if(ready){getSharedPreferences("bridge",0).edit().putBoolean("setup_complete",true).commit();showStep(3);}else startControl("START");});return;}
    try{File f=new File(getFilesDir(),"link-code.json");if(f.exists()){
      JSONObject c=new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));long seconds=c.getLong("expires_at")-System.currentTimeMillis()/1000;
      if(seconds>0){requestingCode=false;code.setText(c.getString("code"));status.setText("Informe este código no vínculo da TCL Assistant. Válido por "+seconds+" segundos. A confirmação aparecerá aqui automaticamente.");primary.setText("Aguardando vínculo…");primary.setEnabled(false);return;}
      if(!requestingCode){code.setText("Código expirado");status.setText("Gere outro código para continuar.");}
    }}catch(Exception ignored){}
    if(!requestingCode&&code.getText().length()>0&&!"Código expirado".contentEquals(code.getText())){code.setText("Código expirado");status.setText("Gere outro código para continuar.");}
    String error=s.optString("setup_error");if(!error.isEmpty()){status.setText(error);requestingCode=false;}
    if(requestingCode&&SystemClock.elapsedRealtime()-requestStarted>45000){requestingCode=false;status.setText("A TCL ainda não respondeu. Confira a conexão da TV e tente novamente.");}
    primary.setText(requestingCode?"Gerando código…":"Gerar meu código");primary.setEnabled(!requestingCode);primary.setOnClickListener(v->generateCode());
  }
  void done(){title("Tudo pronto","O vínculo está salvo nesta TV. A ponte inicia automaticamente após ligar e mantém os controles sem precisar abrir esta tela.");
    body.addView(text("Experimente no Echo:\n\n“Alexa, aumente o volume da TV”\n“Alexa, desligue a TV”"+(PowerCompatibility.enabled(this)?"\n“Alexa, ligue a TV”":""),18,Color.WHITE));
    body.addView(text("Use o nome da TV cadastrado na Alexa. Os tokens são guardados e renovados automaticamente.",14,Color.LTGRAY));
    body.addView(text("A conexão de configuração foi encerrada e a chave privada temporária foi descartada. Você pode desativar a depuração nas configurações da TV.",13,Color.LTGRAY));
    primary=button("Ir para o painel",()->{startActivity(new Intent(this,MainActivity.class));finish();});button("Abrir configurações da TV",()->openDeveloperSettings());
  }
  void openDeveloperSettings(){try{startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){Toast.makeText(this,"Abra as configurações pelo controle remoto",Toast.LENGTH_LONG).show();}}}
  @Override public void onBackPressed(){if(busy){new AlertDialog.Builder(this).setTitle("Cancelar a preparação?").setMessage("Você poderá tentar novamente. Seu vínculo atual será preservado.").setNegativeButton("Continuar",null).setPositiveButton("Cancelar",(d,w)->cancelPreparation()).show();}else if(step>0&&step<3)showStep(step-1);else super.onBackPressed();}
  @Override protected void onSaveInstanceState(Bundle b){b.putInt("step",step);super.onSaveInstanceState(b);}
  @Override protected void onResume(){super.onResume();ui.post(refresh);if(resumed&&step==0)showStep(0);resumed=true;if(getSharedPreferences("bridge",0).getBoolean("pending_native_install",false)&&getPackageManager().canRequestPackageInstalls()){installer=new SmartAlexaInstaller(this);installer.begin();}}
  @Override protected void onPause(){ui.removeCallbacks(refresh);super.onPause();}
  @Override protected void onDestroy(){destroyed=true;LocalAdb active=session;if(active!=null)active.close();if(installer!=null)installer.close();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
