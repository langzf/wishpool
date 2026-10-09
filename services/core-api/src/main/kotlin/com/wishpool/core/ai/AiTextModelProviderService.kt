package com.wishpool.core.ai
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.NotFoundError
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
@Service class AiTextModelProviderService(private val db:JdbcClient,private val mapper:ObjectMapper){
 fun listAdminProviders()=list().map(::response)
 fun resolve(capability:String):AiTextModelProviderRecord?=db.sql("select ${cols()} from ai_text_model_provider where capability=:cap and is_default and is_enabled limit 1").param("cap",capability).query(aiTextModelProviderRecord(mapper)).optional().orElse(null)
 @Transactional fun create(r:AiTextModelProviderWriteRequest)=save(r,null)
 @Transactional fun update(id:UUID,r:AiTextModelProviderWriteRequest):AiTextModelProviderResponse{val old=find(id)?:throw NotFoundError("AI provider not found.");return save(r,id,old.apiKey)}
 @Transactional fun toggle(id:UUID,r:AiTextModelProviderToggleRequest)=db.sql("update ai_text_model_provider set is_enabled=:v,updated_at=now() where id=:id returning ${cols()}").param("v",r.isEnabled).param("id",id).query(aiTextModelProviderRecord(mapper)).optional().orElseThrow{NotFoundError("AI provider not found.")}.let(::response)
 @Transactional fun setDefault(id:UUID):AiTextModelProviderResponse{val p=find(id)?:throw NotFoundError("AI provider not found.");db.sql("update ai_text_model_provider set is_default=false,updated_at=now() where capability=:c").param("c",p.capability).update();return db.sql("update ai_text_model_provider set is_default=true,updated_at=now() where id=:id returning ${cols()}").param("id",id).query(aiTextModelProviderRecord(mapper)).single().let(::response)}
 fun delete(id:UUID){if(db.sql("delete from ai_text_model_provider where id=:id").param("id",id).update()==0)throw NotFoundError("AI provider not found.")}
 fun test(id:UUID):AiTextProviderTestResponse{val p=find(id)?:throw NotFoundError("AI provider not found.");if(p.providerType=="deterministic")return AiTextProviderTestResponse(true,"确定性模型无需连接");return try{val body="{\"model\":\"${p.modelName}\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}]}";val req=HttpRequest.newBuilder(URI.create(p.baseUrl.trimEnd('/')+"/chat/completions")).timeout(Duration.ofSeconds(5)).header("Authorization","Bearer ${p.apiKey.orEmpty()}").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();val s=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.discarding());when(s.statusCode()){in 200..299->AiTextProviderTestResponse(true,"连接成功（HTTP ${s.statusCode()}）");401,403->AiTextProviderTestResponse(false,"鉴权失败：请检查 API Key");404->AiTextProviderTestResponse(false,"接口路径不存在：请检查 Base URL（是否包含 /v1）");else->AiTextProviderTestResponse(false,"连接失败：HTTP ${s.statusCode()}")}}catch(e:Exception){AiTextProviderTestResponse(false,"网络不可达：请检查 Base URL")}}
 private fun list()=db.sql("select ${cols()} from ai_text_model_provider order by capability,code").query(aiTextModelProviderRecord(mapper)).list()
 private fun find(id:UUID)=db.sql("select ${cols()} from ai_text_model_provider where id=:id").param("id",id).query(aiTextModelProviderRecord(mapper)).optional().orElse(null)
 private fun save(r:AiTextModelProviderWriteRequest,id:UUID?,oldKey:String?=null):AiTextModelProviderResponse{if(r.providerType !in setOf("volcengine_ark","aliyun_bailian","openai_compatible","deepseek","deterministic")||r.capability !in setOf("text","vision","text_vision","asr"))throw BadRequestError("Unsupported provider type or capability.");if(r.isDefault)db.sql("update ai_text_model_provider set is_default=false where capability=:c").param("c",r.capability).update();val key=r.apiKey?.trim()?.takeIf{it.isNotEmpty()}?:oldKey;return try{val q=if(id==null)"insert into ai_text_model_provider(code,display_name,provider_type,base_url,api_key,model_name,capability,extra_params,is_default,is_enabled) values(:code,:name,:type,:url,:key,:model,:cap,cast(:extra as jsonb),:def,:en) returning ${cols()}" else "update ai_text_model_provider set display_name=:name,provider_type=:type,base_url=:url,api_key=:key,model_name=:model,capability=:cap,extra_params=cast(:extra as jsonb),is_default=:def,is_enabled=:en,updated_at=now() where id=:id returning ${cols()}";val x=db.sql(q).param("id",id).param("code",r.code?.trim()).param("name",r.displayName.trim()).param("type",r.providerType).param("url",r.baseUrl.trimEnd('/')).param("key",key).param("model",r.modelName).param("cap",r.capability).param("extra",mapper.writeValueAsString(r.extraParams?:mapper.createObjectNode())).param("def",r.isDefault).param("en",r.isEnabled);x.query(aiTextModelProviderRecord(mapper)).single().let(::response)}catch(_:DuplicateKeyException){throw ConflictError("AI provider code already exists.")}}
 private fun response(p:AiTextModelProviderRecord)=AiTextModelProviderResponse(p.id,p.code,p.displayName,p.providerType,p.baseUrl,mask(p.apiKey),!p.apiKey.isNullOrBlank(),p.modelName,p.capability,p.extraParams,p.isDefault,p.isEnabled,p.createdAt,p.updatedAt)
 private fun mask(v:String?):String?=v?.takeIf{it.isNotBlank()}?.let{"${it.take(3)}-****${it.takeLast(4)}"}
 private fun cols()="id,code,display_name,provider_type,base_url,api_key,model_name,capability,extra_params::text as extra_params_json,is_default,is_enabled,created_at,updated_at"
}
