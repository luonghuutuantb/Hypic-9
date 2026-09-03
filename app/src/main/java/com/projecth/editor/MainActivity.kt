package com.projecth.editor

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import androidx.compose.ui.text.font.FontStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import kotlin.math.*

data class EditState(
    val exposure:Float=0f,val contrast:Float=0f,val saturation:Float=0f,val warmth:Float=0f,
    val highlights:Float=0f,val shadows:Float=0f,val brightness:Float=0f,val smooth:Float=0f,
    val skinBright:Float=0f,val teeth:Float=0f,val eyeBright:Float=0f,val eyeSize:Float=0f,
    val faceSlim:Float=0f,val chin:Float=0f,val noseSlim:Float=0f,val mouth:Float=0f,
    val forehead:Float=0f,val cheekLift:Float=0f,val jaw:Float=0f,val eyeLift:Float=0f,val lipSize:Float=0f,val noseHeight:Float=0f,
    val bgBlur:Float=0f,val bgDim:Float=0f,val bgWarmth:Float=0f,val vignette:Float=0f,
    val bodySlim:Float=0f,val waistSlim:Float=0f,val bodyHeight:Float=0f,
    val eraseX:Float=-1f,val eraseY:Float=-1f,val eraseRadius:Float=0f,val bgFeather:Float=.5f,val segStrength:Float=1f,
    val bgPreset:Int=0,val eraseMode:Boolean=false,val filter:FilterType=FilterType.NONE
)
enum class FilterType{NONE,VIVID,FILM,WARM,COOL,FADE,BW}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{ProjectHApp()}}
}

@Composable
fun ProjectHApp(){
    val context=LocalContext.current
    var original by remember{mutableStateOf<Bitmap?>(null)}
    var rendered by remember{mutableStateOf<Bitmap?>(null)}
    var state by remember{mutableStateOf(EditState())}
    var history by remember{mutableStateOf(listOf(EditState()))}
    var index by remember{mutableIntStateOf(0)}
    var tab by remember{mutableStateOf("Adjust")}
    var faces by remember{mutableStateOf<List<DetectedFace>>(emptyList())}
    var selectedFace by remember{mutableIntStateOf(0)}
    var status by remember{mutableStateOf("Chưa phân tích") }
    var detecting by remember{mutableStateOf(false)}
    var segmentation by remember{mutableStateOf<SegmentationResult?>(null)}
    var segmenting by remember{mutableStateOf(false)}
    var layers by remember{mutableStateOf(listOf<EditorLayer>())}
    var layerDialog by remember{mutableStateOf(false)}
    var newText by remember{mutableStateOf("")}
    var activeLayerId by remember{mutableStateOf<Long?>(null)}
    var layerFontSize by remember{mutableStateOf(0.08f)}
    var layerColor by remember{mutableStateOf(Color.White)}
    var layerStroke by remember{mutableStateOf(0f)}
    var layerShadow by remember{mutableStateOf(0f)}
    var stickerPicker by remember{mutableStateOf(false)}
    var smartObjects by remember{mutableStateOf(emptyList<SmartObject>())}
    var smartDetecting by remember{mutableStateOf(false)}
    var batchUris by remember{mutableStateOf<List<Uri>>(emptyList())}
    var batchTemplate by remember{mutableStateOf(BuiltInTemplates.all.first())}
    var batchRunning by remember{mutableStateOf(false)}
    var batchProgress by remember{mutableIntStateOf(0)}
    var batchMessage by remember{mutableStateOf("")}
    var exportFormat by remember{mutableStateOf("JPG")}
    var exportQuality by remember{mutableIntStateOf(95)}
    var exportMaxSide by remember{mutableIntStateOf(0)}
    var exportWatermark by remember{mutableStateOf(false)}
    var exportMessage by remember{mutableStateOf("")}
    val recipeStore=remember{RecipeStore(context)}
    var recipes by remember{mutableStateOf(recipeStore.load())}
    var recipeName by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    val batchPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)){uris->
        batchUris=uris
        batchMessage=if(uris.isEmpty()) "Chưa chọn ảnh" else "Đã chọn ${uris.size} ảnh"
    }

    fun render(){
        original?.let{rendered=renderBitmap(it,state,faces.getOrNull(selectedFace),segmentation,layers)}
    }
    fun commit(s:EditState){
        state=s;history=(history.take(index+1)+s).takeLast(60);index=history.lastIndex;render()
    }

    fun export(){
        rendered?.let{bmp->
            val v=ContentValues().apply{
                put(MediaStore.Images.Media.DISPLAY_NAME,"ProjectH_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ProjectH")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{uri->
                context.contentResolver.openOutputStream(uri)?.use{bmp.compress(Bitmap.CompressFormat.JPEG,95,it)}
            }
        }
    }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->
        uri?:return@rememberLauncherForActivityResult
        val bmp=PerformanceEngine.decodePreview(context,uri)
        bmp?.let{
            original=it;state=EditState();history=listOf(state);index=0;layers=emptyList()
            faces=emptyList();selectedFace=0;status="Đang phân tích ảnh..."
            segmentation=null;segmenting=true
            rendered=renderBitmap(it,state,null,null,layers)
            SegmentationService(context).process(it){ result ->
                segmentation=result
                segmenting=false
                status=if(result!=null) "AI tách nền sẵn sàng" else "Không lấy được mask AI"
                render()
            }
        }
    }

    MaterialTheme(colorScheme=darkColorScheme(background=Color(12,12,15),surface=Color(22,22,26))){
        Column(Modifier.fillMaxSize().background(Color(12,12,15)).statusBarsPadding()){
            Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){
                Text("PROJECT H",Modifier.weight(1f),fontSize=18.sp)
                IconButton(enabled=index>0,onClick={index--;state=history[index];render()}){Icon(Icons.Default.Undo,"Undo")}
                IconButton(enabled=index<history.lastIndex,onClick={index++;state=history[index];render()}){Icon(Icons.Default.Redo,"Redo")}
                IconButton(onClick={export()}){Icon(Icons.Default.Download,"Export")}
            }
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(10.dp)
                    .pointerInput(tab, original, state.eraseRadius){
                        detectTapGestures { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = (offset.x / w).coerceIn(0f, 1f)
                            val ny = (offset.y / h).coerceIn(0f, 1f)
                            if (tab == "Background" && state.eraseMode && original != null && state.eraseRadius > 0f) {
                                commit(state.copy(eraseX = nx, eraseY = ny))
                            } else if (tab == "Layers" && layers.isNotEmpty()) {
                                val hit = layers.asReversed().firstOrNull {
                                    it.visible && kotlin.math.abs(it.x - nx) < 0.18f && kotlin.math.abs(it.y - ny) < 0.12f
                                }
                                if (hit != null) activeLayerId = hit.id
                            }
                        }
                    },
                contentAlignment=Alignment.Center
            ){
                if(rendered==null){
                    Column(horizontalAlignment=Alignment.CenterHorizontally){
                        Icon(Icons.Default.AddPhotoAlternate,null,Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Project H V14",fontSize=20.sp)
                        Text("AI Portrait + Body + Background Studio",color=Color.Gray)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick={picker.launch("image/*")}){Text("Mở ảnh")}
                    }
                }else {
                    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))){
                        BoxWithConstraints(Modifier.fillMaxSize()){
                            Image(rendered!!.asImageBitmap(),"Edited photo",Modifier.fillMaxSize().projectHGpuPreview(),contentScale=ContentScale.Fit)
                            val active=layers.firstOrNull{it.id==activeLayerId} ?: layers.lastOrNull{it.visible}
                            active?.let{ layer->
                                Text(
                                    if(layer.type==LayerType.TEXT) layer.text else layer.sticker,
                                    color=Color(layer.color),
                                    fontSize=(layer.size*maxWidth.value).coerceAtLeast(12f).sp,
                                    fontWeight=FontWeight.Bold,
                                    modifier=Modifier.align(Alignment.Center)
                                        .then(if(layer.id==activeLayerId) Modifier.border(1.dp,Color.White) else Modifier)
                                        .graphicsLayer(
                                        translationX=(layer.x-.5f)*maxWidth.value,
                                        translationY=(layer.y-.5f)*maxHeight.value,
                                        rotationZ=layer.rotation,
                                        scaleX=layer.scale,
                                        scaleY=layer.scale
                                    )
                                )
                            }
                            if(tab=="Background"){
                                Text("Object Eraser: chạm lên vật thể cần xoá",Modifier.align(Alignment.TopCenter).padding(8.dp),
                                    color=Color.White,fontSize=11.sp)
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("Adjust","Beauty","Face","Body","Background","Layers","Templates","Presets","Batch","Export","Filters").forEach{FilterChip(selected=tab==it,onClick={tab=it},label={Text(it)})}
            }

            if(tab=="Face"){
                Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=6.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(status,Modifier.weight(1f),fontSize=11.sp,color=Color.Gray)
                        OutlinedButton(enabled=original!=null && !detecting,onClick={
                            original?.let{bmp->
                                detecting=true
                                status="Đang phân tích bằng AI on-device…"
                                MlKitFaceDetector.detect(context, bmp) { result ->
                                    faces=result
                                    selectedFace=0
                                    detecting=false
                                    status=if(result.isEmpty()) "Không phát hiện mặt" else "${result.size} khuôn mặt • ML Kit on-device"
                                    render()
                                }
                            }
                        }){Text(if(detecting) "Đang quét…" else "AI Detect")}
                    }
                    if(faces.isNotEmpty()){
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            faces.forEachIndexed{i,f->
                                FilterChip(
                                    selected=selectedFace==i,
                                    onClick={selectedFace=i;render()},
                                    label={Text("Mặt ${i+1} • ${(f.confidence*100).roundToInt()}%")}
                                )
                            }
                        }
                    }
                }
            }

            when(tab){
                "Adjust"->AdjustPanel(state,::commit)
                "Beauty"->BeautyPanel(state,::commit)
                "Face"->FacePanel(state,faces.isNotEmpty(),::commit)
                "Body"->BodyPanel(state,::commit)
                "Background"->BackgroundPanel(state,segmentation,smartObjects,smartDetecting,{ if(original!=null){ smartDetecting=true; SmartObjectDetector().detect(original!!){smartObjects=it;smartDetecting=false} } },::commit)
                "Layers"->LayersPanel(layers,{layers=it;render()}, {layerDialog=true}, {stickerPicker=true}, activeLayerId)
                "Templates"->TemplatePanel(batchTemplate,{batchTemplate=it},{commit(applyTemplateToState(batchTemplate,state))})
                "Presets"->PresetPanel(recipes,state,recipeName,{recipeName=it},{r->commit(r.toEditState())},{r->{recipeStore.save(r);recipes=recipeStore.load()}},{n->{recipeStore.delete(n);recipes=recipeStore.load()}})
                "Batch"->BatchPanel(batchUris,batchTemplate,batchRunning,batchProgress,batchMessage,
                    {batchPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                    {batchTemplate=it},{batchUris=emptyList();batchMessage=""},
                    {
                        if(batchUris.isNotEmpty()&&!batchRunning){
                            batchRunning=true;batchProgress=0;batchMessage="Đang xử lý…"
                            scope.launch(Dispatchers.IO){
                                var ok=0;var fail=0
                                batchUris.forEachIndexed{idx,uri->
                                    try{
                                        val bmp=PerformanceEngine.decodePreview(context,uri)
                                        if(bmp!=null){
                                            val out=renderBitmap(bmp,applyTemplateToState(batchTemplate,EditState()),null,null,emptyList())
                                            val msg=saveBitmapToGallery(context,out,"JPG",95,0,false)
                                            if(msg.startsWith("Đã lưu"))ok++ else fail++
                                            if(out!==bmp)out.recycle();bmp.recycle()
                                        }else fail++
                                    }catch(_:Throwable){fail++}
                                    withContext(Dispatchers.Main){batchProgress=idx+1}
                                }
                                withContext(Dispatchers.Main){batchRunning=false;batchMessage="Hoàn tất: $ok thành công • $fail lỗi"}
                            }
                        }
                    })
                "Export"->ExportPanel(exportFormat,exportQuality,exportMaxSide,exportWatermark,exportMessage,
                    {exportFormat=it},{exportQuality=it},{exportMaxSide=it},{exportWatermark=it},{
                        rendered?.let{exportMessage=saveBitmapToGallery(context,it,exportFormat,exportQuality,exportMaxSide,exportWatermark)} ?: run{exportMessage="Chưa có ảnh để xuất"}
                    })
                "Filters"->FilterPanel(state,::commit)
            }
        }
    }

    
    if(stickerPicker){
        AlertDialog(
            onDismissRequest={stickerPicker=false},
            title={Text("Sticker Studio")},
            text={
                Column{
                    Text("Sticker cơ bản, offline",color=Color.Gray,fontSize=12.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        listOf("❤️","⭐","✨","🔥","🌸","☀️","🎉","📸").forEach{st->
                            Text(st,fontSize=30.sp,modifier=Modifier.clickable{
                                layers=layers+EditorLayer(
                                    id=System.nanoTime(),type=LayerType.STICKER,sticker=st,
                                    x=.5f,y=.5f,size=.11f,color=Color.White.value.toInt()
                                )
                                activeLayerId=layers.lastOrNull()?.id
                                stickerPicker=false
                                render()
                            })
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={stickerPicker=false}){Text("Đóng")}}
        )
    }

if(layerDialog){
        AlertDialog(
            onDismissRequest={layerDialog=false},
            title={Text("Thêm Text Layer")},
            text={
                Column{
                    Text("Nội dung",fontSize=12.sp,color=Color.Gray)
                    BasicTextField(
                        value=newText,
                        onValueChange={newText=it},
                        textStyle=TextStyle(color=Color.White,fontSize=18.sp),
                        modifier=Modifier.fillMaxWidth().padding(top=8.dp)
                    )
                }
            },
            confirmButton={
                TextButton(onClick={
                    val txt=newText.trim()
                    if(txt.isNotEmpty()){
                        layers=layers+EditorLayer(
                            id=System.nanoTime(),type=LayerType.TEXT,text=txt,
                            x=.5f,y=.5f,size=layerFontSize,color=layerColor.value.toInt(),
                            strokeWidth=layerStroke,shadow=layerShadow,
                            bold=true
                        )
                        newText=""
                        layerDialog=false
                        render()
                    }
                }){Text("Thêm")}
            },
            dismissButton={TextButton(onClick={layerDialog=false}){Text("Huỷ")}}
        )
    }

}


@Composable fun LayersPanel(
    layers:List<EditorLayer>,
    onChange:(List<EditorLayer>)->Unit,
    addText:()->Unit,
    addSticker:()->Unit,
    activeLayerId:Long?
){
    Column(
        Modifier.fillMaxWidth().heightIn(min=220.dp,max=340.dp)
            .verticalScroll(rememberScrollState()).padding(14.dp)
    ){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text("Layers",fontSize=18.sp,modifier=Modifier.weight(1f))
            Button(onClick=addText){Text("+ Text")}
            Spacer(Modifier.width(6.dp))
            Button(onClick=addSticker){Text("+ Sticker")}
        }
        Text("V15: mỗi text/sticker là một layer độc lập. Có thể ẩn, xoá và thay đổi thứ tự.",color=Color.Gray,fontSize=11.sp)
        Spacer(Modifier.height(8.dp))
        layers.asReversed().forEachIndexed{reverseIndex,layer->
            val realIndex=layers.lastIndex-reverseIndex
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){
                Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(if(layer.type==LayerType.TEXT) "T" else layer.sticker,Modifier.width(30.dp),fontWeight=FontWeight.Bold)
                    Text(if(layer.type==LayerType.TEXT) layer.text else layer.sticker,Modifier.weight(1f))
                    IconButton(onClick={
                        val m=layers.toMutableList();m[realIndex]=layer.copy(visible=!layer.visible);onChange(m)
                    }){Icon(if(layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,"Visibility")}
                    IconButton(onClick={
                        val m=layers.toMutableList();m.removeAt(realIndex);onChange(m)
                    }){Icon(Icons.Default.Delete,"Delete")}
                }
            }
        }
        if(layers.isEmpty()) Text("Chưa có layer. Nhấn + Text để bắt đầu.",color=Color.Gray)
        layers.firstOrNull{it.id==activeLayerId}?.let{layer->
            Spacer(Modifier.height(8.dp))
            Text("Layer đang chọn: ${if(layer.type==LayerType.TEXT) layer.text else layer.sticker}",fontSize=13.sp)
            SliderRow("Size",layer.size,.03f,.20f){v->
                val m=layers.map{if(it.id==layer.id) it.copy(size=v) else it};onChange(m)
            }
            SliderRow("Rotation",layer.rotation,-180f,180f){v->
                val m=layers.map{if(it.id==layer.id) it.copy(rotation=v) else it};onChange(m)
            }
            SliderRow("Opacity",layer.alpha,0f,1f){v->
                val m=layers.map{if(it.id==layer.id) it.copy(alpha=v) else it};onChange(m)
            }
            SliderRow("Stroke",layer.strokeWidth,0f,.025f){v->
                val m=layers.map{if(it.id==layer.id) it.copy(strokeWidth=v) else it};onChange(m)
            }
            SliderRow("Shadow",layer.shadow,0f,.03f){v->
                val m=layers.map{if(it.id==layer.id) it.copy(shadow=v) else it};onChange(m)
            }
            Text("Text Color",fontSize=12.sp,color=Color.Gray)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(Color.White,Color.Black,Color.Red,Color.Yellow,Color.Cyan).forEach{cc->
                    Box(Modifier.size(28.dp).background(cc,CircleShape).clickable{
                        val m=layers.map{if(it.id==layer.id) it.copy(color=cc.value.toInt()) else it};onChange(m)
                    })
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedButton(onClick={

                    val m=layers.map{if(it.id==layer.id) it.copy(bold=!it.bold) else it};onChange(m)
                }){Text(if(layer.bold) "Bold ✓" else "Bold")}
                OutlinedButton(onClick={
                    val m=layers.map{if(it.id==layer.id) it.copy(italic=!it.italic) else it};onChange(m)
                }){Text(if(layer.italic) "Italic ✓" else "Italic")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedButton(onClick={
                    val m=layers.map{if(it.id==layer.id) it.copy(scale=(it.scale-.1f).coerceAtLeast(.2f)) else it};onChange(m)
                }){Text("Scale −")}
                OutlinedButton(onClick={
                    val m=layers.map{if(it.id==layer.id) it.copy(scale=(it.scale+.1f).coerceAtMost(4f)) else it};onChange(m)
                }){Text("Scale +")}
                OutlinedButton(onClick={
                    val m=layers.map{if(it.id==layer.id) it.copy(x=.5f,y=.5f,rotation=0f,scale=1f) else it};onChange(m)
                }){Text("Center")}
            }
        }
    }
}


@Composable
fun ExportPanel(
    format:String, quality:Int, maxSide:Int, watermark:Boolean, message:String,
    setFormat:(String)->Unit,setQuality:(Int)->Unit,setMaxSide:(Int)->Unit,setWatermark:(Boolean)->Unit,
    export:()->Unit
){
    Column(Modifier.fillMaxWidth().heightIn(min=250.dp,max=390.dp).verticalScroll(rememberScrollState()).padding(12.dp)){
        Text("Export Center",fontSize=18.sp)
        Text("Xuất ảnh với chất lượng và kích thước kiểm soát được.",color=Color.Gray,fontSize=11.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("JPG","PNG","WEBP").forEach{f->
                OutlinedButton(onClick={setFormat(f)}){Text(if(format==f) "$f ✓" else f)}
            }
        }
        SliderRow("Quality",quality.toFloat(),40f,100f){setQuality(it.roundToInt())}
        Text("Max side: ${if(maxSide==0) "Original" else "${maxSide}px"}",fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf(0,2048,3000,4096).forEach{v->
                OutlinedButton(onClick={setMaxSide(v)}){Text(if(v==0) "Original" else "${v/1000}K")}
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically){
            Checkbox(checked=watermark,onCheckedChange=setWatermark)
            Text("Watermark: Project H")
        }
        Button(onClick=export,modifier=Modifier.fillMaxWidth()){Text("EXPORT TO GALLERY")}
        if(message.isNotBlank()) Text(message,color=Color.Gray,fontSize=12.sp)
    }
}


@Composable
fun PresetPanel(recipes:List<EditRecipe>, state:EditState, name:String, setName:(String)->Unit, apply:(EditRecipe)->Unit, save:(EditRecipe)->Unit, delete:(String)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=230.dp,max=390.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Preset Studio",fontSize=18.sp)
        Text("Lưu toàn bộ thông số chỉnh ảnh thành công thức và áp dụng lại cho ảnh khác.",fontSize=11.sp,color=Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedTextField(name,setName,label={Text("Tên preset")},singleLine=true,modifier=Modifier.weight(1f))
            Button(onClick={save(state.toRecipe(name.ifBlank{"My Preset"}))}){Text("Lưu")}
        }
        Text("Có sẵn",fontSize=12.sp,color=Color.Gray,modifier=Modifier.padding(top=10.dp))
        EditRecipe.builtIns.forEach{r->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(r.name,Modifier.weight(1f));TextButton(onClick={apply(r)}){Text("Áp dụng")}}}
        if(recipes.isNotEmpty()){Text("Của bạn",fontSize=12.sp,color=Color.Gray,modifier=Modifier.padding(top=8.dp));recipes.forEach{r->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(r.name,Modifier.weight(1f));TextButton(onClick={apply(r)}){Text("Áp dụng")};TextButton(onClick={delete(r.name)}){Text("Xóa")}}}}
    }
}

@Composable fun TemplatePanel(selected:EditorTemplate,onSelect:(EditorTemplate)->Unit,onApply:()->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=190.dp,max=300.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Templates",fontSize=18.sp)
        Text("Preset đồng bộ màu + beauty, có thể áp dụng ngay cho ảnh hiện tại hoặc Batch.",fontSize=11.sp,color=Color.Gray)
        Spacer(Modifier.height(8.dp))
        BuiltInTemplates.all.forEach{t->
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp).clickable{onSelect(t)}){
                Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(t.emoji,fontSize=22.sp);Spacer(Modifier.width(10.dp));Text(t.name,Modifier.weight(1f));if(selected.id==t.id)Text("✓")
                }
            }
        }
        Button(onClick=onApply,modifier=Modifier.fillMaxWidth()){Text("ÁP DỤNG CHO ẢNH HIỆN TẠI")}
    }
}

@Composable fun BatchPanel(uris:List<Uri>,template:EditorTemplate,running:Boolean,progress:Int,message:String,onPick:()->Unit,onTemplate:(EditorTemplate)->Unit,onClear:()->Unit,onRun:()->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=230.dp,max=380.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Text("Batch Export",fontSize=18.sp,modifier=Modifier.weight(1f));Text("${uris.size}/20 ảnh",fontSize=12.sp,color=Color.Gray)}
        Text("Chọn nhiều ảnh, áp dụng cùng template và xuất hàng loạt vào Pictures/ProjectH.",fontSize=11.sp,color=Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Button(onClick=onPick,enabled=!running){Text("+ Chọn ảnh")}
            OutlinedButton(onClick=onClear,enabled=!running){Text("Xoá")}
        }
        Text("Template",fontSize=12.sp,color=Color.Gray)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            BuiltInTemplates.all.forEach{t->FilterChip(selected=template.id==t.id,onClick={onTemplate(t)},label={Text("${t.emoji} ${t.name}")})}
        }
        if(uris.isNotEmpty()) Text("Tiến trình: $progress/${uris.size}",fontSize=12.sp)
        LinearProgressIndicator(progress={if(uris.isEmpty())0f else progress.toFloat()/uris.size},Modifier.fillMaxWidth())
        Button(onClick=onRun,enabled=uris.isNotEmpty()&&!running,modifier=Modifier.fillMaxWidth()){Text(if(running) "ĐANG XUẤT…" else "XUẤT TẤT CẢ")}
        if(message.isNotBlank()) Text(message,fontSize=12.sp,color=Color.Gray)
    }
}

fun applyTemplateToState(t:EditorTemplate,base:EditState):EditState=base.copy(
    exposure=t.exposure,contrast=t.contrast,saturation=t.saturation,warmth=t.warmth,
    skinBright=t.skinBright,smooth=t.smooth,filter=t.filter
)

@Composable fun AdjustPanel(s:EditState,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=220.dp,max=330.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Adjust",fontSize=18.sp)
        SliderRow("Brightness",s.brightness,-1f,1f){c(s.copy(brightness=it))}
        SliderRow("Exposure",s.exposure,-1f,1f){c(s.copy(exposure=it))}
        SliderRow("Contrast",s.contrast,-1f,1f){c(s.copy(contrast=it))}
        SliderRow("Saturation",s.saturation,-1f,1f){c(s.copy(saturation=it))}
        SliderRow("Warmth",s.warmth,-1f,1f){c(s.copy(warmth=it))}
        SliderRow("Highlights",s.highlights,-1f,1f){c(s.copy(highlights=it))}
        SliderRow("Shadows",s.shadows,-1f,1f){c(s.copy(shadows=it))}
    }
}
@Composable fun BeautyPanel(s:EditState,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=220.dp,max=330.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Beauty",fontSize=18.sp)
        SliderRow("Smooth",s.smooth,0f,1f){c(s.copy(smooth=it))}
        SliderRow("Skin Light",s.skinBright,0f,1f){c(s.copy(skinBright=it))}
        SliderRow("Teeth",s.teeth,0f,1f){c(s.copy(teeth=it))}
        SliderRow("Eye Bright",s.eyeBright,0f,1f){c(s.copy(eyeBright=it))}
    }
}
@Composable fun FacePanel(s:EditState,detected:Boolean,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=220.dp,max=350.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Face Warp",fontSize=18.sp)
        Text(if(detected)"Đã có landmarks cho khuôn mặt đang chọn." else "Nhấn AI Detect để tìm khuôn mặt.",color=Color.Gray,fontSize=12.sp)
        SliderRow("Face Slim",s.faceSlim,0f,1f){c(s.copy(faceSlim=it))}
        SliderRow("Eye Size",s.eyeSize,0f,1f){c(s.copy(eyeSize=it))}
        SliderRow("Nose Slim",s.noseSlim,0f,1f){c(s.copy(noseSlim=it))}
        SliderRow("Chin",s.chin,-1f,1f){c(s.copy(chin=it))}
        SliderRow("Mouth Lift",s.mouth,-1f,1f){c(s.copy(mouth=it))}
        SliderRow("Forehead",s.forehead,-1f,1f){c(s.copy(forehead=it))}
        SliderRow("Cheek Lift",s.cheekLift,-1f,1f){c(s.copy(cheekLift=it))}
        SliderRow("Jawline",s.jaw,-1f,1f){c(s.copy(jaw=it))}
        SliderRow("Eye Lift",s.eyeLift,-1f,1f){c(s.copy(eyeLift=it))}
        SliderRow("Lip Size",s.lipSize,-1f,1f){c(s.copy(lipSize=it))}
        SliderRow("Nose Height",s.noseHeight,-1f,1f){c(s.copy(noseHeight=it))}
    }
}

@Composable fun BodyPanel(s:EditState,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=210.dp,max=330.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Body Shape",fontSize=18.sp)
        Text("V12 dùng warp hình học nhẹ theo vùng thân, chưa phải body segmentation AI.",color=Color.Gray,fontSize=11.sp)
        SliderRow("Body Slim",s.bodySlim,-1f,1f){c(s.copy(bodySlim=it))}
        SliderRow("Waist Slim",s.waistSlim,-1f,1f){c(s.copy(waistSlim=it))}
        SliderRow("Body Height",s.bodyHeight,-1f,1f){c(s.copy(bodyHeight=it))}
    }
}

@Composable fun BackgroundPanel(s:EditState,seg:SegmentationResult?,objects:List<SmartObject>,detecting:Boolean,scan:()->Unit,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=230.dp,max=360.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Background & Retouch",fontSize=18.sp)
        Text("V18: AI Object Detection + Smart Eraser.",color=Color.Gray,fontSize=11.sp)
        Text("Phát hiện tối đa 5 object để chọn nhanh vùng cần xoá.",color=Color.Gray,fontSize=11.sp)
        Button(onClick=scan){Text(if(detecting) "Đang quét..." else "Smart Scan")}
        if(objects.isNotEmpty()){
            Text("Objects:",fontSize=12.sp,color=Color.Gray)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                objects.forEach{obj->
                    AssistChip(
                        onClick={
                            val cx=obj.bounds.centerX().toFloat()/100000f
                            val cy=obj.bounds.centerY().toFloat()/100000f
                            c(s.copy(eraseX=cx.coerceIn(0f,1f),eraseY=cy.coerceIn(0f,1f),eraseMode=true))
                        },
                        label={Text("${obj.label} ${(obj.confidence*100).roundToInt()}%")}
                    )
                }
            }
        }
        Text(if(seg==null) "Mask: chưa phân tích" else "Mask: AI foreground đã sẵn sàng", color=Color.Gray,fontSize=11.sp)
        SliderRow("BG Blur",s.bgBlur,0f,1f){c(s.copy(bgBlur=it))}
        SliderRow("BG Dim",s.bgDim,0f,1f){c(s.copy(bgDim=it))}
        SliderRow("BG Warmth",s.bgWarmth,-1f,1f){c(s.copy(bgWarmth=it))}
        SliderRow("Vignette",s.vignette,0f,1f){c(s.copy(vignette=it))}
        SliderRow("Mask Feather",s.bgFeather,0f,1f){c(s.copy(bgFeather=it))}
        SliderRow("Segmentation Strength",s.segStrength,0f,1f){c(s.copy(segStrength=it))}
        SliderRow("Eraser Size",s.eraseRadius,0f,.25f){c(s.copy(eraseRadius=it))}
        Text("Background Replace",fontSize=14.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("Original","Studio","Sunset","Night").forEachIndexed{idx,label->
                OutlinedButton(onClick={c(s.copy(bgPreset=idx))},modifier=Modifier.weight(1f)){Text(label,fontSize=11.sp)}
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick={c(s.copy(eraseMode=!s.eraseMode))}){Text(if(s.eraseMode) "Eraser: ON" else "Eraser: OFF")}
            OutlinedButton(onClick={c(s.copy(eraseX=-1f,eraseY=-1f))}){Text("Clear")}
            OutlinedButton(onClick={c(s.copy(bgBlur=0f,bgDim=0f,bgWarmth=0f,vignette=0f,bgPreset=0,eraseX=-1f,eraseY=-1f,eraseMode=false))}){Text("Reset")}
        }
    }
}

@Composable fun FilterPanel(s:EditState,c:(EditState)->Unit){
    Column(Modifier.fillMaxWidth().heightIn(min=170.dp,max=260.dp).verticalScroll(rememberScrollState()).padding(14.dp)){
        Text("Filters",fontSize=18.sp);Spacer(Modifier.height(8.dp))
        FilterType.values().toList().chunked(3).forEach{row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                row.forEach{f->OutlinedButton(onClick={c(s.copy(filter=f))},modifier=Modifier.weight(1f)){Text(f.name.replace("_"," "))}}
                repeat(3-row.size){Spacer(Modifier.weight(1f))}
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}
@Composable fun SliderRow(n:String,v:Float,min:Float,max:Float,on:(Float)->Unit){
    Row(verticalAlignment=Alignment.CenterVertically){
        Text(n,Modifier.width(88.dp),fontSize=13.sp)
        Slider(v,on,Modifier.weight(1f),valueRange=min..max)
        Text("${(v*100).roundToInt()}",Modifier.width(32.dp),fontSize=11.sp,color=Color.Gray)
    }
}

fun renderBitmap(source:Bitmap,e:EditState,face:DetectedFace?,seg:SegmentationResult?=null,layers:List<EditorLayer> = emptyList()):Bitmap{
    val landmarks=face?.landmarks
    val out=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(out);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    val exp=Math.pow(2.0,e.exposure.toDouble()).toFloat()
    val c=1f+e.contrast;val t=e.brightness*80f+(1f-c)*128f
    val sat=(1f+e.saturation).coerceAtLeast(0f)
    val cm=ColorMatrix(floatArrayOf(exp*c,0f,0f,0f,t,0f,exp*c,0f,0f,t,0f,0f,exp*c,0f,t,0f,0f,0f,1f,0f))
    cm.postConcat(ColorMatrix().apply{setSaturation(sat)})
    val warm=e.warmth*28f
    cm.postConcat(ColorMatrix(floatArrayOf(1f,0f,0f,0f,warm,0f,1f,0f,0f,0f,0f,0f,1f,0f,-warm,0f,0f,0f,1f,0f)))
    when(e.filter){
        FilterType.VIVID->cm.postConcat(ColorMatrix(floatArrayOf(1.12f,0f,0f,0f,-6f,0f,1.08f,0f,0f,-4f,0f,0f,1.12f,0f,-6f,0f,0f,0f,1f,0f)))
        FilterType.FILM->cm.postConcat(ColorMatrix(floatArrayOf(.96f,0f,0f,0f,10f,0f,.96f,0f,0f,8f,0f,0f,.90f,0f,12f,0f,0f,0f,1f,0f)))
        FilterType.WARM->cm.postConcat(ColorMatrix(floatArrayOf(1.06f,0f,0f,0f,8f,0f,1.01f,0f,0f,2f,0f,0f,.94f,0f,-5f,0f,0f,0f,1f,0f)))
        FilterType.COOL->cm.postConcat(ColorMatrix(floatArrayOf(.94f,0f,0f,0f,-4f,0f,1f,0f,0f,0f,0f,0f,1.08f,0f,8f,0f,0f,0f,1f,0f)))
        FilterType.FADE->cm.postConcat(ColorMatrix(floatArrayOf(.92f,0f,0f,0f,18f,0f,.92f,0f,0f,18f,0f,0f,.92f,0f,18f,0f,0f,0f,1f,0f)))
        FilterType.BW->cm.setSaturation(0f)
        else->Unit
    }
    paint.colorFilter=ColorMatrixColorFilter(cm)
    canvas.drawBitmap(source,0f,0f,paint)
    val warped:Bitmap = if(
        landmarks != null &&
        (e.faceSlim>0f || abs(e.eyeSize)>0f || abs(e.noseSlim)>0f || abs(e.chin)>0f ||
         abs(e.mouth)>0f || abs(e.forehead)>0f || abs(e.cheekLift)>0f || abs(e.jaw)>0f ||
         abs(e.eyeLift)>0f || abs(e.lipSize)>0f || abs(e.noseHeight)>0f)
    ) {
        FaceWarpEngine.warp(
            out,
            landmarks,
            FaceWarpParams(e.faceSlim,e.eyeSize,e.noseSlim,e.chin,e.mouth,e.forehead,e.cheekLift,e.jaw,e.eyeLift,e.lipSize,e.noseHeight),
            face?.contours
        )
    } else {
        out
    }
    var finalBmp=applyBeauty(warped,e,face)
    finalBmp=applyBodyWarp(finalBmp,e,face)
    finalBmp=applyBackgroundAndRetouch(finalBmp,e,face,seg)
    return finalBmp
}
fun applyBeauty(out:Bitmap,e:EditState,face:DetectedFace?):Bitmap{
    val w=out.width;val h=out.height;val px=IntArray(w*h);out.getPixels(px,0,w,0,0,w,h)
    val smooth=e.smooth.coerceIn(0f,1f);val light=e.skinBright.coerceIn(0f,1f)
    val teeth=e.teeth.coerceIn(0f,1f);val eye=e.eyeBright.coerceIn(0f,1f)
    if(face!=null && (smooth>0f||light>0f||teeth>0f||eye>0f)){
        val contours=face.contours
        for(i in px.indices){
            val col=px[i];val r=AndroidColor.red(col);val g=AndroidColor.green(col);val b=AndroidColor.blue(col)
            val x=i%w; val y=i/w
            val inFace=FaceMaskEngine.isFace(x,y,face)
            if(!inFace) continue
            val mx = max(r, max(g, b))
            val skin = r > g && g > b && r > 70 && (r - g) < 100 && (g - b) < 90
            var rr = r.toFloat()
            var gg = g.toFloat()
            var bb = b.toFloat()
            if (skin) {
                val sm = smooth * .16f
                val lift = light * 28f
                rr += lift
                gg += lift * .92f
                bb += lift * .78f
                rr += (128f - rr) * sm
                gg += (128f - gg) * sm
                bb += (128f - bb) * sm
            }
            if (contours != null) {
                val eyeRegion = FaceMaskEngine.inFeature(x, y, contours.leftEye) || FaceMaskEngine.inFeature(x, y, contours.rightEye)
                val lipRegion = FaceMaskEngine.inFeature(x, y, contours.upperLipTop) || FaceMaskEngine.inFeature(x, y, contours.upperLipBottom) || FaceMaskEngine.inFeature(x, y, contours.lowerLipTop) || FaceMaskEngine.inFeature(x, y, contours.lowerLipBottom)
                if (eyeRegion && eye > 0f) {
                    val boost = eye * 24f
                    rr += boost
                    gg += boost
                    bb += boost
                }
                if (lipRegion && teeth > 0f) {
                    val neutral = abs(r - g) < 28 && abs(g - b) < 28 && mx > 120
                    if (neutral) {
                        val boost = teeth * 18f
                        rr += boost
                        gg += boost
                        bb += boost
                    }
                }
            } else if (abs(r - g) < 28 && abs(g - b) < 28 && mx > 120) {
                val boost = max(teeth, eye) * 18f
                rr += boost
                gg += boost
                bb += boost
            }
            px[i] = AndroidColor.argb(
                AndroidColor.alpha(col),
                rr.coerceIn(0f, 255f).roundToInt(),
                gg.coerceIn(0f, 255f).roundToInt(),
                bb.coerceIn(0f, 255f).roundToInt()
            )
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
    }
    return out
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, format: String, quality: Int, maxSide: Int, watermark: Boolean): String {
    var out = bitmap
    if (maxSide > 0) {
        val scale = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height))
        if (scale < .999f) out = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
    }
    if (watermark) {
        val copy = out.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(170, 255, 255, 255)
            textSize = (copy.width * .028f).coerceAtLeast(18f)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Project H", copy.width - 24f, copy.height - 24f, p)
        out = copy
    }
    val mime = when (format) {
        "PNG" -> "image/png"
        "WEBP" -> "image/webp"
        else -> "image/jpeg"
    }
    val ext = when (format) {
        "PNG" -> "png"
        "WEBP" -> "webp"
        else -> "jpg"
    }
    val name = "ProjectH_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date()) + "." + ext
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (android.os.Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectH")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return "Không tạo được file"
    context.contentResolver.openOutputStream(uri).use { os ->
        if (os == null) return "Không mở được output"
        val compress = when (format) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.JPEG
        }
        out.compress(compress, quality, os)
    }
    return "Đã lưu: $name"
}

private fun portraitMask(x: Float, y: Float, w: Int, h: Int, face: DetectedFace?): Float {
    if (face == null) return 0f
    val b = face.bounds
    // Expanded portrait corridor: head + shoulders/chest.
    val cx = b.centerX
    val cy = (b.centerY + b.height * .62f).coerceAtMost(h.toFloat())
    val rx = b.width * .95f
    val ry = b.height * 1.65f
    val dx = (x - cx) / rx
    val dy = (y - cy) / ry
    return (1f - (dx * dx + dy * dy)).coerceIn(0f, 1f)
}

private fun applyBodyWarp(src: Bitmap, e: EditState, face: DetectedFace?): Bitmap {
    if (face == null || (abs(e.bodySlim) < .001f && abs(e.waistSlim) < .001f && abs(e.bodyHeight) < .001f)) return src
    val w = src.width
    val h = src.height
    val sp = IntArray(w * h)
    src.getPixels(sp, 0, w, 0, 0, w, h)
    val dp = IntArray(w * h)
    val cx = face.bounds.centerX
    val top = face.bounds.bottom.toFloat()
    val bodyH = (h - top).coerceAtLeast(face.bounds.height * 1.2f)
    fun sample(x: Float, y: Float): Int {
        val ix = x.roundToInt().coerceIn(0, w - 1)
        val iy = y.roundToInt().coerceIn(0, h - 1)
        return sp[iy * w + ix]
    }
    for (y in 0 until h) {
        for (x in 0 until w) {
            val rel = ((y - top) / bodyH).coerceIn(0f, 1.3f)
            val shoulder = exp(-((rel - .18f) / .28f).pow(2))
            val waist = exp(-((rel - .48f) / .24f).pow(2))
            var sx = x.toFloat()
            var sy = y.toFloat()
            val slim = (e.bodySlim * 0.12f * shoulder + e.waistSlim * 0.18f * waist)
            sx = cx + (sx - cx) * (1f - slim)
            sy += e.bodyHeight * .045f * bodyH * exp(-((rel - .55f) / .65f).pow(2))
            dp[y * w + x] = sample(sx, sy)
        }
    }
    src.setPixels(dp, 0, w, 0, 0, w, h)
    return src
}

private fun applyBackgroundAndRetouch(src: Bitmap, e: EditState, face: DetectedFace?, seg: SegmentationResult? = null): Bitmap {
    if (e.bgBlur <= .001f && e.bgDim <= .001f && abs(e.bgWarmth) <= .001f && e.vignette <= .001f &&
        e.eraseX < 0f && e.bgPreset == 0) return src

    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    val radius = (1 + e.bgBlur * 6f).roundToInt()

    fun avg(x: Int, y: Int): IntArray {
        var rr = 0
        var gg = 0
        var bb = 0
        var n = 0
        for (yy in max(0, y - radius)..min(h - 1, y + radius)) {
            for (xx in max(0, x - radius)..min(w - 1, x + radius)) {
                val q = px[yy * w + xx]
                rr += AndroidColor.red(q)
                gg += AndroidColor.green(q)
                bb += AndroidColor.blue(q)
                n++
            }
        }
        return intArrayOf(rr / n, gg / n, bb / n)
    }

    fun bgColor(x: Int, y: Int): IntArray {
        val u = x.toFloat() / max(1, w - 1)
        val v = y.toFloat() / max(1, h - 1)
        return when (e.bgPreset) {
            1 -> { // studio
                val q = (v * 28).roundToInt()
                intArrayOf(242 - q, 242 - q, 242 - q)
            }
            2 -> { // sunset
                intArrayOf((245 - 70 * v).roundToInt(), (170 - 50 * v).roundToInt(), (110 - 30 * v).roundToInt())
            }
            3 -> { // night
                intArrayOf((35 + 30 * (1 - v)).roundToInt(), (48 + 35 * (1 - u)).roundToInt(), (78 + 55 * (1 - v)).roundToInt())
            }
            else -> intArrayOf(0, 0, 0)
        }
    }

    for (i in px.indices) {
        val x = i % w
        val y = i / w
        val heuristic = portraitMask(x.toFloat(), y.toFloat(), w, h, face)
        val ai = (seg?.at(x, y) ?: heuristic).coerceIn(0f, 1f)
        val strength = e.segStrength.coerceIn(0f, 1f)
        val raw = (heuristic * (1f - strength) + ai * strength).coerceIn(0f, 1f)
        val pm = (raw * (1f - e.bgFeather * .35f) + heuristic * (e.bgFeather * .35f)).coerceIn(0f, 1f)

        var r = AndroidColor.red(px[i]).toFloat()
        var g = AndroidColor.green(px[i]).toFloat()
        var b = AndroidColor.blue(px[i]).toFloat()

        val bg = (1f - pm)
        if (e.bgPreset > 0) {
            val c = bgColor(x, y)
            r = r * pm + c[0] * bg
            g = g * pm + c[1] * bg
            b = b * pm + c[2] * bg
        }

        val blurAmount=bg*e.bgBlur.coerceIn(0f,1f)
        if(blurAmount>.001f){
            val a=avg(x,y)
            r=r*(1f-blurAmount)+a[0]*blurAmount
            g=g*(1f-blurAmount)+a[1]*blurAmount
            b=b*(1f-blurAmount)+a[2]*blurAmount
        }

        val dim=bg*e.bgDim.coerceIn(0f,1f)*.55f
        r*=1f-dim; g*=1f-dim; b*=1f-dim

        val warm=e.bgWarmth.coerceIn(-1f,1f)*12f*bg
        r+=warm; b-=warm*.8f

        if(e.vignette>.001f){
            val nx=(x-w/2f)/(w/2f); val ny=(y-h/2f)/(h/2f)
            val v=((nx*nx+ny*ny)/2f).coerceIn(0f,1f)*e.vignette*.32f
            r*=1f-v;g*=1f-v;b*=1f-v
        }

        out[i]=AndroidColor.rgb(r.coerceIn(0f,255f).roundToInt(),
            g.coerceIn(0f,255f).roundToInt(),b.coerceIn(0f,255f).roundToInt())
    }

    // V14 spot healing: feathered clone from a nearby offset patch.
    if(e.eraseX>=0f && e.eraseY>=0f && e.eraseRadius>.001f){
        val cx=(e.eraseX*w).roundToInt().coerceIn(0,w-1)
        val cy=(e.eraseY*h).roundToInt().coerceIn(0,h-1)
        val rad=(e.eraseRadius*max(w,h)).roundToInt().coerceIn(2,min(w,h)/3)
        val ox=(rad*2).coerceAtMost(max(2,w/5))
        for(y in max(0,cy-rad)..min(h-1,cy+rad))
            for(x in max(0,cx-rad)..min(w-1,cx+rad)){
                val d=hypot((x-cx).toFloat(),(y-cy).toFloat())
                if(d<=rad){
                    val a=((1f-d/rad).coerceIn(0f,1f)).pow(.65f)
                    val sx=(x+ox).coerceIn(0,w-1)
                    val sy=y
                    val q=px[sy*w+sx]
                    val base=out[y*w+x]
                    out[y*w+x]=AndroidColor.rgb(
                        (AndroidColor.red(base)*(1-a)+AndroidColor.red(q)*a).roundToInt().coerceIn(0,255),
                        (AndroidColor.green(base)*(1-a)+AndroidColor.green(q)*a).roundToInt().coerceIn(0,255),
                        (AndroidColor.blue(base)*(1-a)+AndroidColor.blue(q)*a).roundToInt().coerceIn(0,255)
                    )
                }
            }
    }

    src.setPixels(out,0,w,0,0,w,h)
    if(e.eraseX>=0f && e.eraseY>=0f && e.eraseRadius>.001f){
        val cx=(e.eraseX*w).roundToInt().coerceIn(0,w-1)
        val cy=(e.eraseY*h).roundToInt().coerceIn(0,h-1)
        val rad=(e.eraseRadius*max(w,h)).roundToInt().coerceIn(2,min(w,h)/3)
        return SmartEraserEngine.heal(src,cx,cy,rad,.65f)
    }
    return src
}
