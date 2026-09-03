package com.projecth.editor

import org.json.JSONObject

data class EditRecipe(
    val name:String, val brightness:Float=0f, val exposure:Float=0f, val contrast:Float=0f, val saturation:Float=0f, val warmth:Float=0f,
    val highlights:Float=0f, val shadows:Float=0f, val smooth:Float=0f, val skinBright:Float=0f, val teeth:Float=0f, val eyeBright:Float=0f,
    val eyeSize:Float=0f, val faceSlim:Float=0f, val chin:Float=0f, val noseSlim:Float=0f, val mouth:Float=0f, val forehead:Float=0f,
    val cheekLift:Float=0f, val jaw:Float=0f, val eyeLift:Float=0f, val lipSize:Float=0f, val noseHeight:Float=0f,
    val bgBlur:Float=0f, val bgDim:Float=0f, val bgWarmth:Float=0f, val vignette:Float=0f, val bodySlim:Float=0f, val waistSlim:Float=0f, val bodyHeight:Float=0f,
    val bgFeather:Float=.5f, val segStrength:Float=1f, val filter:FilterType=FilterType.NONE
){
    fun toJson():String=JSONObject().apply{
        put("name",name); put("brightness",brightness); put("exposure",exposure); put("contrast",contrast); put("saturation",saturation); put("warmth",warmth); put("highlights",highlights); put("shadows",shadows)
        put("smooth",smooth); put("skinBright",skinBright); put("teeth",teeth); put("eyeBright",eyeBright); put("eyeSize",eyeSize); put("faceSlim",faceSlim); put("chin",chin); put("noseSlim",noseSlim); put("mouth",mouth); put("forehead",forehead); put("cheekLift",cheekLift); put("jaw",jaw); put("eyeLift",eyeLift); put("lipSize",lipSize); put("noseHeight",noseHeight)
        put("bgBlur",bgBlur); put("bgDim",bgDim); put("bgWarmth",bgWarmth); put("vignette",vignette); put("bodySlim",bodySlim); put("waistSlim",waistSlim); put("bodyHeight",bodyHeight); put("bgFeather",bgFeather); put("segStrength",segStrength); put("filter",filter.name)
    }.toString()
    companion object{
        fun fromJson(s:String):EditRecipe{ val o=JSONObject(s); return EditRecipe(
            o.optString("name","Preset"), o.f("brightness"),o.f("exposure"),o.f("contrast"),o.f("saturation"),o.f("warmth"),o.f("highlights"),o.f("shadows"),o.f("smooth"),o.f("skinBright"),o.f("teeth"),o.f("eyeBright"),o.f("eyeSize"),o.f("faceSlim"),o.f("chin"),o.f("noseSlim"),o.f("mouth"),o.f("forehead"),o.f("cheekLift"),o.f("jaw"),o.f("eyeLift"),o.f("lipSize"),o.f("noseHeight"),o.f("bgBlur"),o.f("bgDim"),o.f("bgWarmth"),o.f("vignette"),o.f("bodySlim"),o.f("waistSlim"),o.f("bodyHeight"),o.optDouble("bgFeather",.5).toFloat(),o.optDouble("segStrength",1.0).toFloat(),runCatching{FilterType.valueOf(o.optString("filter","NONE"))}.getOrDefault(FilterType.NONE)) }
        private fun JSONObject.f(k:String)=optDouble(k,0.0).toFloat()
        val builtIns=listOf(
            EditRecipe("Clean"),
            EditRecipe("Film",contrast=.08f,saturation=-.06f,warmth=.03f,highlights=-.08f),
            EditRecipe("Warm",exposure=.08f,warmth=.14f,saturation=.05f),
            EditRecipe("Cool",exposure=.03f,warmth=-.14f,saturation=.02f),
            EditRecipe("Portrait",exposure=.06f,contrast=.04f,saturation=.03f,smooth=.12f,skinBright=.08f,eyeBright=.06f),
            EditRecipe("Mono",saturation=-1f,contrast=.10f,filter=FilterType.BW)
        )
    }
}

fun EditState.toRecipe(name:String="My Preset")=EditRecipe(name,brightness,exposure,contrast,saturation,warmth,highlights,shadows,smooth,skinBright,teeth,eyeBright,eyeSize,faceSlim,chin,noseSlim,mouth,forehead,cheekLift,jaw,eyeLift,lipSize,noseHeight,bgBlur,bgDim,bgWarmth,vignette,bodySlim,waistSlim,bodyHeight,bgFeather,segStrength,filter)
fun EditRecipe.toEditState()=EditState(brightness,exposure,contrast,saturation,warmth,highlights,shadows,smooth,skinBright,teeth,eyeBright,eyeSize,faceSlim,chin,noseSlim,mouth,forehead,cheekLift,jaw,eyeLift,lipSize,noseHeight,bgBlur,bgDim,bgWarmth,vignette,bodySlim,waistSlim,bodyHeight,-1f,-1f,0f,bgFeather,segStrength,0,false,filter)
