package local.tclbridge;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;

public class MainActivity extends Activity {
  final Handler handler=new Handler();TextView headline,account,cloud,executor,standby,code,details;LinearLayout overview,diagnostic;Button toggle;
  boolean diagnostics=false;JSONObject state=new JSONObject();
  final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};
  int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
  LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  @Override public void onCreate(Bundle saved){super.onCreate(saved);getExternalFilesDir(null);
    LinearLayout root=new LinearLayout(this);root.setPadding(dp(32),dp(30),dp(32),dp(24));root.setBackgroundColor(Color.rgb(18,22,28));
    LinearLayout menu=column();menu.setPadding(0,0,dp(26),0);root.addView(menu,new LinearLayout.LayoutParams(0,-1,.34f));
    TextView name=text("Alexa na TV",25,Color.WHITE);name.setTypeface(null,Typeface.BOLD);menu.addView(name);
    TextView subtitle=text("Controle por voz",12,Color.LTGRAY);subtitle.setPadding(0,dp(6),0,dp(24));menu.addView(subtitle);
    Button home=button(menu,"Visão geral",()->show(false));home.setSelected(true);
    toggle=button(menu,"Iniciar",()->{boolean running=getSharedPreferences("bridge",0).getBoolean("enabled",false);start(running?"PAUSE":"START");});
    button(menu,"Vincular conta",()->{show(false);start("LINK");});
    button(menu,"Reconectar",()->start("RECONNECT"));
    button(menu,"Configurações",()->settings());
    button(menu,"Diagnóstico",()->show(true));
    TextView foot=text("Alexa e sua TV, conectadas",10,Color.rgb(145,155,169));foot.setPadding(0,dp(18),0,0);menu.addView(foot);
    View divider=new View(this);divider.setBackgroundColor(Color.rgb(44,51,61));root.addView(divider,new LinearLayout.LayoutParams(dp(1),-1));
    LinearLayout pane=column();pane.setPadding(dp(30),0,0,0);root.addView(pane,new LinearLayout.LayoutParams(0,-1,.66f));
    TextView title=text("Controle sua TV com Alexa",27,Color.WHITE);pane.addView(title);
    headline=text("Verificando conexão…",16,Color.LTGRAY);headline.setPadding(0,dp(12),0,dp(26));pane.addView(headline);
    overview=column();pane.addView(overview);
    account=statusRow(overview,"Conta Alexa");cloud=statusRow(overview,"Conexão com a TCL");executor=statusRow(overview,"Controles TCL");standby=statusRow(overview,"Despertar por rede");
    code=text("",22,Color.WHITE);code.setPadding(0,dp(28),0,dp(10));code.setVisibility(View.GONE);overview.addView(code);
    TextView instructions=text("Use seu Echo para controlar volume, entradas, aplicativos e energia.\nA disponibilidade de canais, reprodução e gravação depende dos recursos da TV.",13,Color.rgb(155,165,180));instructions.setPadding(0,dp(24),0,0);overview.addView(instructions);
    diagnostic=column();diagnostic.setVisibility(View.GONE);pane.addView(diagnostic,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout actions=new LinearLayout(this);diagnostic.addView(actions);
    button(actions,"Testar volume",()->start("SELF_TEST"));button(actions,"Estado do TTVSCore do sistema",()->start("NATIVE_STATUS"));
    ScrollView scroll=new ScrollView(this);details=text("",12,Color.LTGRAY);details.setPadding(0,dp(14),0,0);scroll.addView(details);diagnostic.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);home.requestFocus();
    if(getSharedPreferences("bridge",0).getBoolean("enabled",false))start("START");
  }
  TextView statusRow(LinearLayout parent,String label){LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(11),0,dp(11));
    TextView key=text(label,16,Color.rgb(200,208,218));row.addView(key,new LinearLayout.LayoutParams(0,-2,1));TextView value=text("…",15,Color.LTGRAY);value.setGravity(Gravity.RIGHT);row.addView(value);parent.addView(row);return value;
  }
  Button button(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(dp(14),0,dp(14),0);
    StateListDrawable background=new StateListDrawable();background.addState(new int[]{android.R.attr.state_focused},new ColorDrawable(Color.rgb(51,77,101)));background.addState(new int[]{android.R.attr.state_pressed},new ColorDrawable(Color.rgb(51,77,101)));background.addState(new int[]{},new ColorDrawable(Color.TRANSPARENT));b.setBackground(background);
    b.setOnClickListener(v->action.run());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(parent.getOrientation()==LinearLayout.VERTICAL?-1:-2,dp(46));params.bottomMargin=dp(3);parent.addView(b,params);return b;
  }
  String read(String file)throws Exception{return new String(java.nio.file.Files.readAllBytes(new File(getFilesDir(),file).toPath()),"UTF-8");}
  void indicator(TextView view,boolean ok,String good,String pending){view.setText(ok?good:pending);view.setTextColor(ok?Color.rgb(140,218,174):Color.rgb(210,190,146));}
  void render(){try{state=new JSONObject(read("state.json"));}catch(Exception ignored){}
    boolean enabled=getSharedPreferences("bridge",0).getBoolean("enabled",false),linked=state.optBoolean("linked"),connected=state.optBoolean("connected"),ready=state.optBoolean("executor");
    boolean draining=state.optBoolean("draining"),resume=state.optBoolean("resume_requested");
    toggle.setText(draining?(resume?"Cancelar retomada":"Retomar após salvar"):(enabled?"Pausar controle":"Iniciar controle"));
    headline.setText(!enabled?"Controle pausado":connected&&ready?"Pronta para seus comandos":linked?"Conta vinculada • reconectando":"Vincule sua conta para começar");
    if(draining)headline.setText(state.optBoolean("drain_uncertain")?"Aguardando a renovação da conta • sem cancelar":resume?"Salvando a conta • retomada solicitada":"Salvando a conta • controle pausado");
    if(!state.optString("configuration_error").isEmpty())headline.setText("Configure o perfil desta TV antes de vincular");
    headline.setTextColor(!draining&&connected&&ready?Color.rgb(140,218,174):Color.LTGRAY);
    indicator(account,linked,"Vinculada","Aguardando vínculo");indicator(cloud,connected,"Conectada","Desconectada");indicator(executor,ready,"Disponíveis","Inicializando");indicator(standby,state.optBoolean("network_standby")&&state.optBoolean("standby_exempt"),"Ativo","Revisar energia");
    try{File f=new File(getFilesDir(),"link-code.json");if(f.exists()&&!linked){JSONObject c=new JSONObject(read("link-code.json"));long seconds=c.getLong("expires_at")-System.currentTimeMillis()/1000;
      code.setText(seconds>0?"Código de ativação\n\n"+c.getString("code")+"\n\nInforme na skill TCL Assistant • "+seconds+" s":"Código expirado. Selecione Vincular conta.");code.setVisibility(View.VISIBLE);
    }else code.setVisibility(View.GONE);}catch(Exception ignored){}
    if(diagnostics)try{details.setText("Configuração: "+state.optString("configuration_error","OK")+"\n"+"Ponte TTVSCore → SmartAlexa\nExceção de energia: "+state.optBoolean("standby_exempt")+"\nEstado privado salvo: "+state.optBoolean("persisted")+"\n\n"+"Pausa em andamento: "+state.optBoolean("draining")+"; operações pendentes: "+state.optInt("pending_subscriptions")+"\nRetornos nativos: "+state.optJSONObject("metrics")+"\n\n"+read("status.txt"));}catch(Exception ignored){}
  }
  void show(boolean diagnosticMode){diagnostics=diagnosticMode;overview.setVisibility(diagnosticMode?View.GONE:View.VISIBLE);diagnostic.setVisibility(diagnosticMode?View.VISIBLE:View.GONE);render();}
  void start(String action){getSharedPreferences("bridge",0).edit().putBoolean("enabled",!"PAUSE".equals(action)).apply();startForegroundService(new Intent(this,BridgeService.class).setAction(action));}
  void settings(){boolean boot=getSharedPreferences("bridge",0).getBoolean("autostart",true);
    new AlertDialog.Builder(this).setTitle("Configurações").setItems(new String[]{"Iniciar após ligar a TV: "+(boot?"ativado":"desativado"),"Ativar despertar por rede","Permissões de exibição","Atualizar nome e dados da TV","Adicionar outra TV à conta","Desvincular conta Alexa","Manter Alexa em espera: "+(getSharedPreferences("bridge",0).getBoolean("standby_compatibility",false)?"ativado":"desativado")},(d,which)->{
      if(which==0){getSharedPreferences("bridge",0).edit().putBoolean("autostart",!boot).apply();RecoveryJob.schedule(this);toast("Inicialização automática "+(!boot?"ativada":"desativada"));}
      if(which==1){if(Settings.System.getInt(getContentResolver(),"tcl_sys_network_standby_enabled",0)==1){toast("Despertar por rede já está ativado");}else{toast("Ative espera por rede nas configurações de energia da TV");open(new Intent(Settings.ACTION_SETTINGS));}}
      if(which==2)open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
      if(which==3)start("UPDATE_PROFILE");
      if(which==4)groupDialog();
      if(which==6){boolean next=!PowerCompatibility.enabled(this);try{PowerCompatibility.setEnabled(this,next);toast(next?"Alexa em espera ativada; pode aumentar o consumo":"Configuração anterior de energia restaurada");}catch(RuntimeException e){toast("Configuração de instalação pendente");}}

      if(which==5)new AlertDialog.Builder(this).setTitle("Desvincular esta TV?").setMessage("O vínculo desta TV será removido da conta Alexa. Será necessário gerar outro código para conectar novamente.").setNegativeButton("Cancelar",null).setPositiveButton("Desvincular",(a,b)->start("LOGOUT")).show();
    }).setNegativeButton("Fechar",null).show();
  }
  void groupDialog(){EditText input=new EditText(this);input.setHint("Código de seis dígitos");input.setInputType(InputType.TYPE_CLASS_NUMBER);new AlertDialog.Builder(this).setTitle("Adicionar outra TV").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Vincular",(d,w)->{
    String code=input.getText().toString().trim();if(!code.matches("[0-9]{6}")){toast("Informe seis dígitos");return;}getSharedPreferences("bridge",0).edit().putBoolean("enabled",true).apply();startForegroundService(new Intent(this,BridgeService.class).setAction("BIND_GROUP").putExtra("code",code));
  }).show();}
  void open(Intent i){try{startActivity(i);}catch(Exception e){toast("Configuração indisponível nesta TV");}}
  void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
  @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);}
  @Override protected void onResume(){super.onResume();handler.post(refresh);}
  @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
