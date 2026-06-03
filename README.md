# EXPRESS CONSUMO OC - Android Nativo V4 Correção Definitiva de Extração

Correção principal: PDFBox Android agora usa `setSortByPosition(true)`, igual ao modo layout do PDF.
Isso mantém produto, quantidade prevista, per capita e unidade na mesma linha, corrigindo o erro “Poucos registros extraídos”.

Também inclui:

- processamento em segundo plano para evitar travamento;
- geração automática de `debug_texto_extraido.txt` quando a extração ficar baixa;
- fallback sem filtro de data quando o período digitado não bate com o PDF;
- abertura de PDFs pelo Meus Arquivos;
- escolha de pasta de saída;
- geração de PDF consumo, PDF faltantes, PDF sobras e XLSX.

## GitHub Actions

Actions > Build APK Android Nativo - Express Consumo OC > Run workflow
