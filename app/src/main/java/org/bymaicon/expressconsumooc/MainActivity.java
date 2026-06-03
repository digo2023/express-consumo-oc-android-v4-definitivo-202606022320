package org.bymaicon.expressconsumooc;

import android.app.*;import android.os.*;import android.content.*;import android.net.*;import android.provider.DocumentsContract;import android.graphics.*;import android.graphics.pdf.PdfDocument;import android.view.*;import android.widget.*;import android.text.*;import android.text.method.*;import android.graphics.drawable.*;import android.content.res.ColorStateList;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;import com.tom_roush.pdfbox.pdmodel.PDDocument;import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;import java.text.*;import java.util.*;import java.util.regex.*;import java.util.zip.*;

public class MainActivity extends Activity{
    static final int REQ_ANALITICO=10, REQ_OC=11, REQ_FOLDER=12;
    Uri analiticoUri, ocUri, folderUri;
    TextView status, log; EditText dataIni, dataFim; String analiticoText="", ocText="";
    ArrayList<Item> itensConsumo=new ArrayList<>(), itensOC=new ArrayList<>();
    int azul=Color.rgb(0,45,120), dourado=Color.rgb(245,166,20), fundo=Color.rgb(2,8,24);
    SimpleDateFormat br=new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR"));

    public void onCreate(Bundle b){super.onCreate(b); PDFBoxResourceLoader.init(getApplicationContext()); montarTela();}
    TextView tv(String s,int sp,int color,int style){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,style);v.setPadding(16,8,16,8);return v;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{azul,Color.rgb(0,95,190)});g.setCornerRadius(28);b.setBackground(g);b.setPadding(12,8,12,8);return b;}
    public void montarTela(){
        ScrollView sv=new ScrollView(this); LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(fundo);sv.addView(root);
        ImageView logo=new ImageView(this);logo.setImageResource(getResources().getIdentifier("icon","drawable",getPackageName()));logo.setAdjustViewBounds(true);logo.setMaxHeight(320);root.addView(logo,new LinearLayout.LayoutParams(-1,260));
        TextView title=tv("EXPRESS CONSUMO OC",28,Color.WHITE,Typeface.BOLD);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=tv("Cálculo de Consumo • Conferência de Ordem de Compra",14,dourado,Typeface.BOLD);sub.setGravity(Gravity.CENTER);root.addView(sub);
        LinearLayout linha=new LinearLayout(this);linha.setOrientation(LinearLayout.HORIZONTAL);linha.setPadding(0,8,0,8);root.addView(linha);
        dataIni=new EditText(this);dataFim=new EditText(this); for(EditText e:new EditText[]{dataIni,dataFim}){e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.setSingleLine(true);e.setTextSize(15);e.setBackgroundTintList(ColorStateList.valueOf(dourado));}
        dataIni.setHint("Início dd/mm/aaaa");dataFim.setHint("Fim dd/mm/aaaa");linha.addView(dataIni,new LinearLayout.LayoutParams(0,-2,1));linha.addView(dataFim,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(btn("📄 Escolher PDF Analítico")); ((Button)root.getChildAt(root.getChildCount()-1)).setOnClickListener(v->abrirArquivo(REQ_ANALITICO));
        root.addView(btn("🧾 Escolher PDF Ordem de Compra")); ((Button)root.getChildAt(root.getChildCount()-1)).setOnClickListener(v->abrirArquivo(REQ_OC));
        root.addView(btn("📁 Escolher pasta para salvar relatórios")); ((Button)root.getChildAt(root.getChildCount()-1)).setOnClickListener(v->abrirPasta());
        root.addView(btn("📊 Gerar consumo semanal")); ((Button)root.getChildAt(root.getChildCount()-1)).setOnClickListener(v->gerarConsumo());
        root.addView(btn("✅ Conferir Ordem de Compra")); ((Button)root.getChildAt(root.getChildCount()-1)).setOnClickListener(v->conferirOC());
        status=tv("Pronto. Selecione os arquivos e a pasta de saída.",15,Color.WHITE,Typeface.BOLD);root.addView(status);
        log=tv("",13,Color.LTGRAY,Typeface.NORMAL);log.setMovementMethod(new ScrollingMovementMethod());root.addView(log);
        TextView rod=tv("dd/mm/aaaa - By Maicon",12,Color.LTGRAY,Typeface.NORMAL);rod.setGravity(Gravity.RIGHT);root.addView(rod);
        setContentView(sv);
    }
    void abrirArquivo(int req){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");startActivityForResult(i,req);} 
    void abrirPasta(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);startActivityForResult(i,REQ_FOLDER);} 
    protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null)return; try{Uri u=d.getData(); if(r==REQ_ANALITICO){analiticoUri=u; analiticoText=lerPdf(u); status.setText("Analítico carregado."); append("Analítico OK: "+u); sugerirDatas();} else if(r==REQ_OC){ocUri=u; ocText=lerPdf(u); status.setText("OC carregada."); append("OC OK: "+u);} else if(r==REQ_FOLDER){folderUri=u; getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); append("Pasta de saída OK.");}}catch(Exception e){erro(e);}}
    String lerPdf(Uri uri)throws Exception{
        InputStream in=getContentResolver().openInputStream(uri);
        PDDocument doc=PDDocument.load(in);
        PDFTextStripper st=new PDFTextStripper();
        // CORREÇÃO CRÍTICA: sem sortByPosition o PDFBox Android lê primeiro a coluna de produtos
        // e depois a coluna de quantidades, causando "poucos registros extraídos".
        // Com sortByPosition(true), a linha volta a ficar: CÓDIGO + PRODUTO + QTDE PREVISTA + PER CAPITA + UN.
        st.setSortByPosition(true);
        st.setShouldSeparateByBeads(false);
        st.setLineSeparator("\n");
        st.setWordSeparator(" ");
        String t=st.getText(doc);
        doc.close();
        in.close();
        return t==null?"":t;
    }
    void sugerirDatas(){Matcher m=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})").matcher(analiticoText);ArrayList<String> ds=new ArrayList<>();while(m.find())if(!ds.contains(m.group(1)))ds.add(m.group(1)); if(ds.size()>0){dataIni.setText(ds.get(0));dataFim.setText(ds.get(ds.size()-1));}}
    void gerarConsumo(){
        new Thread(()->{
            try{
                runOnUiThread(()->{status.setText("Processando consumo semanal..."); append("Processando consumo semanal...");});
                validarBase(false);
                itensConsumo=extrairAnalitico(analiticoText);
                if(itensConsumo.size()<20){salvarDebugTexto(); throw new Exception("Poucos registros extraídos. Gere/Envie o arquivo debug_texto_extraido.txt para revisão.");}
                Map<String,Item> cons=consolidar(itensConsumo);
                salvarPdfConsumo(cons,"relatorio_consumo_semanal.pdf");
                salvarXlsx(cons,"relatorio_consumo_semanal.xlsx",null,null);
                runOnUiThread(()->{status.setText("Consumo gerado com sucesso."); append("Registros: "+itensConsumo.size()+" | Itens: "+cons.size());});
            }catch(Exception e){runOnUiThread(()->erro(e));}
        }).start();
    }
    void conferirOC(){
        new Thread(()->{
            try{
                runOnUiThread(()->{status.setText("Processando conferência da OC..."); append("Processando conferência da OC...");});
                validarBase(true);
                if(itensConsumo.isEmpty())itensConsumo=extrairAnalitico(analiticoText);
                if(itensConsumo.size()<20){salvarDebugTexto(); throw new Exception("Poucos registros do analítico. Gere/Envie debug_texto_extraido.txt para revisão.");}
                itensOC=extrairOC(ocText);
                Map<String,Item> cons=consolidar(itensConsumo), oc=consolidarOC(itensOC);
                ArrayList<Row> falt=new ArrayList<>(), sobra=new ArrayList<>();
                comparar(cons,oc,falt,sobra);
                salvarPdfConf(falt,"relatorio_itens_faltantes.pdf","RELATÓRIO DE ITENS FALTANTES");
                salvarPdfConf(sobra,"relatorio_itens_sobras.pdf","RELATÓRIO DE SOBRAS / EXCEDENTES");
                salvarXlsx(cons,"conferencia_oc.xlsx",falt,sobra);
                runOnUiThread(()->{status.setText("Conferência finalizada."); append("OC itens: "+oc.size()+" | Faltantes: "+falt.size()+" | Sobras: "+sobra.size());});
            }catch(Exception e){runOnUiThread(()->erro(e));}
        }).start();
    }
    void salvarDebugTexto(){
        try{
            if(folderUri==null||analiticoText==null||analiticoText.isEmpty())return;
            OutputStream os=out("debug_texto_extraido.txt","text/plain");
            String cab="DEBUG TEXTO EXTRAÍDO DO PDF - Express Consumo OC\n"+
                    "Data app: "+dataIni.getText()+" a "+dataFim.getText()+"\n"+
                    "Tamanho caracteres: "+analiticoText.length()+"\n\n";
            os.write(cab.getBytes("UTF-8"));
            os.write(analiticoText.getBytes("UTF-8"));
            os.close();
        }catch(Exception ignored){}
    }
    void validarBase(boolean precisaOC)throws Exception{if(analiticoText.isEmpty())throw new Exception("Selecione o PDF analítico."); if(precisaOC&&ocText.isEmpty())throw new Exception("Selecione o PDF da OC."); if(folderUri==null)throw new Exception("Escolha a pasta para salvar."); if(dataIni.getText().toString().trim().isEmpty()||dataFim.getText().toString().trim().isEmpty())throw new Exception("Informe início e fim da semana.");}
    Date parse(String s)throws Exception{return br.parse(s.trim());} boolean entre(String d,Date a,Date b){try{Date x=parse(d);return !x.before(a)&&!x.after(b);}catch(Exception e){return false;}}
    ArrayList<Item> extrairAnalitico(String t)throws Exception{
        /*
         * EXTRATOR V3 ROBUSTO
         * Motivo da correção: no Android/PDFBox algumas linhas do PDF vêm quebradas
         * ou com a ordem visual diferente. A versão antiga dependia de uma linha
         * perfeita; quando o texto vinha quebrado, extraía poucos registros.
         * Agora o app procura os itens no texto completo e busca DATA/TURNO no
         * contexto anterior ao item.
         */
        ArrayList<Item> out=new ArrayList<>();
        Date ini=parse(dataIni.getText().toString()), fim=parse(dataFim.getText().toString());
        String texto=t.replace('\u00A0',' ').replace("\r","\n").replaceAll("[ \t]+"," ");

        // Regex do item real do analítico: CÓDIGO + PRODUTO + QTDE PREVISTA + PER CAPITA + UNIDADE
        Pattern itemP=Pattern.compile(
            "(?is)\\b(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+"+
            "(.{3,160}?)\\s+"+
            "([0-9.]+,[0-9]{2,5})\\s+"+
            "([0-9.]+,[0-9]{2,5})\\s+"+
            "(KG|LT|UN|PC|CX)\\b"
        );

        Matcher im=itemP.matcher(texto);
        while(im.find()){
            String codigo=im.group(1);
            String nome=limparNomeAnalitico(im.group(2));
            String qtdPrevista=im.group(3);
            String un=im.group(5).toUpperCase(Locale.ROOT);

            // Proteção contra falso item: ignora cabeçalho/rodapé e nomes curtos demais
            if(nome.length()<3) continue;
            if(nome.contains("QTDE")||nome.contains("RELATORIO")||nome.contains("RELATÓRIO")) continue;

            Contexto ctx=contextoAntes(texto, im.start(), ini, fim);
            if(ctx==null) continue;

            Item it=new Item();
            it.codigo=codigo;
            it.nome=nome;
            it.qtd=num(qtdPrevista); // SEMPRE a primeira quantidade após o produto: Qtde. Prevista
            it.un=un;
            it.data=ctx.data;
            it.turno=ctx.turno;
            it.categoria=cat(it.nome,it.codigo);
            out.add(it);
        }

        // Fallback linha a linha, caso algum PDF venha perfeito mas não bata no modo global
        if(out.size()<20){
            ArrayList<Item> linha=extrairAnaliticoLinhaALinha(texto,ini,fim);
            if(linha.size()>out.size()) out=linha;
        }
        // Se o período digitado não bater com o PDF, tenta extrair sem filtro de datas para evitar falso erro.
        // O status mostrará as datas sugeridas automaticamente quando o PDF for selecionado.
        if(out.size()<20){
            ArrayList<Item> todos=extrairAnaliticoLinhaALinhaSemFiltro(texto);
            if(todos.size()>out.size()) out=todos;
        }
        return out;
    }

    static class Contexto{String data="",turno="";Contexto(String d,String t){data=d;turno=t;}}

    Contexto contextoAntes(String texto,int pos,Date ini,Date fim){
        int iniCtx=Math.max(0,pos-6000);
        String ctx=texto.substring(iniCtx,pos);

        String dataEncontrada="";
        Matcher dm=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s*(segunda-feira|terça-feira|quarta-feira|quinta-feira|sexta-feira|sábado|domingo)?",Pattern.CASE_INSENSITIVE).matcher(ctx);
        while(dm.find()){
            String d=dm.group(1);
            if(entre(d,ini,fim)) dataEncontrada=d;
        }
        if(dataEncontrada.isEmpty()) return null;

        String turno="";
        Matcher sm=Pattern.compile("(\\d{5})\\s+(ALMOCO|ALMOÇO|JANTAR|CEIA)\\b",Pattern.CASE_INSENSITIVE).matcher(ctx);
        while(sm.find()){
            String s=sm.group(2).toUpperCase(Locale.ROOT);
            if(s.contains("ALMO")) turno="A";
            else if(s.contains("JANT")) turno="B";
            else if(s.contains("CEIA")) turno="C";
        }
        return new Contexto(dataEncontrada,turno);
    }

    ArrayList<Item> extrairAnaliticoLinhaALinha(String t,Date ini,Date fim){
        ArrayList<Item> out=new ArrayList<>();
        String data="",turno="";
        Pattern dataP=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda-feira|terça-feira|quarta-feira|quinta-feira|sexta-feira|sábado|domingo)?",Pattern.CASE_INSENSITIVE);
        Pattern itemP=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+([0-9.]+,[0-9]{2,5})\\s+([0-9.]+,[0-9]{2,5})\\s+(KG|LT|UN|PC|CX)\\b.*$",Pattern.CASE_INSENSITIVE);
        String pendente="";
        for(String raw:t.split("\\n")){
            String line=raw.trim().replaceAll("\\s+"," ");
            Matcher dm=dataP.matcher(line);
            if(dm.find()&&entre(dm.group(1),ini,fim)) data=dm.group(1);
            String up=line.toUpperCase(Locale.ROOT);
            if(up.matches(".*\\b(ALMOCO|ALMOÇO)\\b.*"))turno="A";
            else if(up.matches(".*\\bJANTAR\\b.*"))turno="B";
            else if(up.matches(".*\\bCEIA\\b.*"))turno="C";

            if(line.matches("^\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}.*")) pendente=line;
            else if(!pendente.isEmpty()) pendente=pendente+" "+line;
            else continue;

            Matcher im=itemP.matcher(pendente);
            if(im.matches()&&!data.isEmpty()){
                Item it=new Item();it.codigo=im.group(1);it.nome=limparNomeAnalitico(im.group(2));it.qtd=num(im.group(3));it.un=im.group(5).toUpperCase(Locale.ROOT);it.data=data;it.turno=turno;it.categoria=cat(it.nome,it.codigo);out.add(it);pendente="";
            }
            if(pendente.length()>260) pendente="";
        }
        return out;
    }


    ArrayList<Item> extrairAnaliticoLinhaALinhaSemFiltro(String t){
        ArrayList<Item> out=new ArrayList<>();
        String data="",turno="";
        Pattern dataP=Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(segunda-feira|terça-feira|quarta-feira|quinta-feira|sexta-feira|sábado|domingo)?",Pattern.CASE_INSENSITIVE);
        Pattern itemP=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+([0-9.]+,[0-9]{2,5})\\s+([0-9.]+,[0-9]{2,5})\\s+(KG|LT|UN|PC|CX)\\b.*$",Pattern.CASE_INSENSITIVE);
        String pendente="";
        for(String raw:t.split("\\n")){
            String line=raw.trim().replaceAll("\\s+"," ");
            Matcher dm=dataP.matcher(line);
            if(dm.find()) data=dm.group(1);
            String up=line.toUpperCase(Locale.ROOT);
            if(up.matches(".*\\b(ALMOCO|ALMOÇO)\\b.*"))turno="A";
            else if(up.matches(".*\\bJANTAR\\b.*"))turno="B";
            else if(up.matches(".*\\bCEIA\\b.*"))turno="C";
            if(line.matches("^\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}.*")) pendente=line;
            else if(!pendente.isEmpty()) pendente=pendente+" "+line;
            else continue;
            Matcher im=itemP.matcher(pendente);
            if(im.matches()&&!data.isEmpty()){
                Item it=new Item();it.codigo=im.group(1);it.nome=limparNomeAnalitico(im.group(2));it.qtd=num(im.group(3));it.un=im.group(5).toUpperCase(Locale.ROOT);it.data=data;it.turno=turno;it.categoria=cat(it.nome,it.codigo);out.add(it);pendente="";
            }
            if(pendente.length()>300) pendente="";
        }
        return out;
    }

    String limparNomeAnalitico(String s){
        String x=s.replace('\u00A0',' ').replaceAll("\\s+"," ").trim().toUpperCase(Locale.ROOT);
        // Remove sobras de colunas caso o PDFBox misture números depois do nome
        x=x.replaceAll("\\s+[0-9.]+,[0-9]{2,5}\\s*$","");
        x=x.replaceAll("^(PRODUTO|TOTAL|UN)\\s+","");
        return x.trim();
    }

    ArrayList<Item> extrairOC(String t){ArrayList<Item> out=new ArrayList<>();Pattern p=Pattern.compile("^(\\d\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2})\\s+(.+?)\\s+(KG|LT|UN|PC|CX)\\s+([0-9.]+,[0-9]{3})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d{2}/\\d{2}/\\d{4}).*$");for(String raw:t.split("\\n")){String line=raw.trim().replaceAll("\\s+"," ");Matcher m=p.matcher(line);if(m.matches()){Item it=new Item();it.codigo=m.group(1);it.nome=limparNomeOC(m.group(2));it.un=m.group(3);it.qtd=num(m.group(4))*fatorOC(m.group(2),it.un);it.data=m.group(6);it.categoria=cat(it.nome,it.codigo);out.add(it);}}return out;}
    double fatorOC(String nome,String un){Matcher m=Pattern.compile("(\\d+)\\s*(KG|UN|LT)\\s*X\\s*(CX|PC|BJ)",Pattern.CASE_INSENSITIVE).matcher(nome);if(m.find())return Double.parseDouble(m.group(1)); if(nome.toUpperCase().contains("30 UN X 12"))return 360; return 1;}
    String limparNome(String s){return s.replaceAll("\\s+"," ").trim().toUpperCase(Locale.ROOT);} String limparNomeOC(String s){return limparNome(s).replaceAll("\\b\\d+\\s*(KG|UN|LT|G)\\s*X\\s*(CX|PC|BJ)\\b","").replaceAll("\\s+"," ").trim();}
    double num(String s){return Double.parseDouble(s.replace(".","").replace(",","."));}
    String cat(String n,String c){String u=n.toUpperCase(); if(c.startsWith("1.01")||c.startsWith("1.11")||u.matches(".*(CARNE|FRANGO|BOVINA|SUINA|LINGUICA|CALABRESA|HAMBURGUER|SASSAMI|OVO|QUEIJO|KIBE|BACON|FEIJOADA).*"))return "PROTEÍNAS"; if(c.startsWith("1.17")||u.matches(".*(ALFACE|TOMATE|CEBOLA|ALHO|BANANA|LARANJA|MACA|MAMÃO|MAMAO|ABACAXI|COUVE|MORANGA).*"))return "HORTIFRUTI"; return "NÃO PERECÍVEIS";}
    String key(Item it){return it.codigo+"|"+norm(it.nome)+"|"+it.un;} String keyOC(Item it){return it.codigo+"|"+norm(it.nome)+"|"+it.un;} String norm(String s){return s.toUpperCase().replaceAll("[^A-Z0-9 ]","").replaceAll("\\s+"," ").trim();}
    Map<String,Item> consolidar(ArrayList<Item> l){LinkedHashMap<String,Item> m=new LinkedHashMap<>();for(Item it:l){String k=key(it);Item x=m.get(k);if(x==null){x=it.copy();m.put(k,x);}else x.qtd+=it.qtd;}return m;}
    Map<String,Item> consolidarOC(ArrayList<Item> l){LinkedHashMap<String,Item> m=new LinkedHashMap<>();for(Item it:l){String k=keyOC(it);Item x=m.get(k);if(x==null){x=it.copy();m.put(k,x);}else x.qtd+=it.qtd;}return m;}
    void comparar(Map<String,Item> cons,Map<String,Item> oc,ArrayList<Row> falt,ArrayList<Row> sobra){HashSet<String> usados=new HashSet<>();for(String k:cons.keySet()){Item a=cons.get(k), b=oc.get(k); if(b==null)b=acharSimilar(a,oc,usados); if(b!=null)usados.add(keyOC(b)); double comprado=b==null?0:b.qtd; double dif=a.qtd-comprado; if(dif>0.001)falt.add(new Row(a,b,a.qtd,comprado,dif,"FALTANTE")); else if(dif<-0.001)sobra.add(new Row(a,b,a.qtd,comprado,-dif,"SOBRA"));} for(String k:oc.keySet())if(!usados.contains(k)){Item b=oc.get(k);sobra.add(new Row(null,b,0,b.qtd,b.qtd,"NÃO PREVISTO NO ANALÍTICO"));}}
    Item acharSimilar(Item a,Map<String,Item> oc,Set<String> usados){for(String k:oc.keySet()){if(usados.contains(k))continue;Item b=oc.get(k);if(!a.un.equals(b.un))continue; if(a.codigo.equals(b.codigo))return b; String an=norm(a.nome),bn=norm(b.nome); if(an.equals(bn))return b;}return null;}
    Uri criar(String nome,String mime)throws Exception{return DocumentsContract.createDocument(getContentResolver(),folderUri,mime,nome);} OutputStream out(String nome,String mime)throws Exception{return getContentResolver().openOutputStream(criar(nome,mime));}
    void salvarPdfConsumo(Map<String,Item> cons,String nome)throws Exception{ArrayList<Item> list=new ArrayList<>(cons.values());Collections.sort(list,Comparator.comparing((Item i)->i.categoria).thenComparing(i->i.nome));PdfDocument pdf=new PdfDocument();Paint p=new Paint();int page=1,y=0;PdfDocument.Page pg=null;Canvas c=null;for(int i=0;i<list.size();i++){if(pg==null||y>760){if(pg!=null)pdf.finishPage(pg);pg=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,page++).create());c=pg.getCanvas();y=40;p.setColor(fundo);c.drawRect(0,0,595,842,p);p.setColor(Color.WHITE);p.setTextSize(19);p.setFakeBoldText(true);c.drawText("RELATÓRIO DE CONSUMO SEMANAL",55,y,p);y+=28;p.setTextSize(10);p.setFakeBoldText(false);c.drawText(dataIni.getText()+" a "+dataFim.getText()+" - By Maicon",380,820,p);p.setColor(dourado);p.setTextSize(12);c.drawText("Produto",35,y,p);c.drawText("UN",390,y,p);c.drawText("Total",445,y,p);y+=18;}Item it=list.get(i);p.setColor(Color.WHITE);p.setTextSize(9);c.drawText(corta(it.nome,48),35,y,p);c.drawText(it.un,390,y,p);c.drawText(fmt(it.qtd),445,y,p);y+=15;}if(pg!=null)pdf.finishPage(pg);OutputStream os=out(nome,"application/pdf");pdf.writeTo(os);os.close();pdf.close();}
    void salvarPdfConf(ArrayList<Row> rows,String nome,String titulo)throws Exception{PdfDocument pdf=new PdfDocument();Paint p=new Paint();int page=1,y=0;PdfDocument.Page pg=null;Canvas c=null;for(int i=0;i<rows.size();i++){if(pg==null||y>760){if(pg!=null)pdf.finishPage(pg);pg=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,page++).create());c=pg.getCanvas();y=40;p.setColor(Color.WHITE);c.drawRect(0,0,595,842,p);p.setColor(azul);p.setTextSize(18);p.setFakeBoldText(true);c.drawText(titulo,50,y,p);y+=28;p.setColor(Color.BLACK);p.setTextSize(8);p.setFakeBoldText(true);c.drawText("PRODUTO",25,y,p);c.drawText("UN",300,y,p);c.drawText("NEC.",330,y,p);c.drawText("OC",390,y,p);c.drawText("DIF.",450,y,p);c.drawText("OBS",505,y,p);y+=14;p.setFakeBoldText(false);c.drawText(dataIni.getText()+" a "+dataFim.getText()+" - By Maicon",410,820,p);}Row r=rows.get(i);p.setColor(Color.BLACK);p.setTextSize(7);c.drawText(corta(r.nome(),50),25,y,p);c.drawText(r.un(),300,y,p);c.drawText(fmt(r.necessario),330,y,p);c.drawText(fmt(r.comprado),390,y,p);c.drawText(fmt(r.diferenca),450,y,p);c.drawText(corta(r.obs,18),505,y,p);y+=13;}if(pg!=null)pdf.finishPage(pg);OutputStream os=out(nome,"application/pdf");pdf.writeTo(os);os.close();pdf.close();}
    void salvarXlsx(Map<String,Item> cons,String nome,ArrayList<Row> falt,ArrayList<Row> sobra)throws Exception{ByteArrayOutputStream baos=new ByteArrayOutputStream();ZipOutputStream z=new ZipOutputStream(baos);zip(z,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");zip(z,"_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");zip(z,"xl/workbook.xml","<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Relatorio\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");zip(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");StringBuilder s=new StringBuilder("<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");int r=1;row(s,r++,"Produto","UN","Necessário","Comprado","Diferença","Obs"); if(falt==null){for(Item it:cons.values())row(s,r++,it.nome,it.un,fmt(it.qtd),"","",it.categoria);} else {for(Row x:falt)row(s,r++,x.nome(),x.un(),fmt(x.necessario),fmt(x.comprado),fmt(x.diferenca),x.obs); for(Row x:sobra)row(s,r++,x.nome(),x.un(),fmt(x.necessario),fmt(x.comprado),fmt(x.diferenca),x.obs);}s.append("</sheetData></worksheet>");zip(z,"xl/worksheets/sheet1.xml",s.toString());z.close();OutputStream os=out(nome,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");os.write(baos.toByteArray());os.close();}
    void zip(ZipOutputStream z,String name,String data)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(data.getBytes("UTF-8"));z.closeEntry();} void row(StringBuilder s,int r,String...vals){s.append("<row r=\"").append(r).append("\">");for(int i=0;i<vals.length;i++){s.append("<c t=\"inlineStr\"><is><t>").append(xml(vals[i])).append("</t></is></c>");}s.append("</row>");}
    String xml(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    String fmt(double d){return String.format(new Locale("pt","BR"),"%,.3f",d);} String corta(String s,int n){return s.length()>n?s.substring(0,n-1):s;} void append(String s){log.append("\n"+s);} void erro(Exception e){status.setText("Erro: "+e.getMessage());append("ERRO: "+e.toString());}
    static class Item{String codigo="",nome="",un="",data="",turno="",categoria="";double qtd;Item copy(){Item i=new Item();i.codigo=codigo;i.nome=nome;i.un=un;i.data=data;i.turno=turno;i.categoria=categoria;i.qtd=qtd;return i;}}
    static class Row{Item a,b;double necessario,comprado,diferenca;String obs;Row(Item a,Item b,double n,double c,double d,String o){this.a=a;this.b=b;necessario=n;comprado=c;diferenca=d;obs=o;}String nome(){return a!=null?a.nome:b.nome;}String un(){return a!=null?a.un:b.un;}}
}
