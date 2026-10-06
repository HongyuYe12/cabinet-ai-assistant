package com.cabinetai.cad;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/cad")
public class DxfController {
 private final DxfDesignService service;
 public DxfController(DxfDesignService service){this.service=service;}
 @PostMapping(value="/design",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<byte[]> design(@RequestParam MultipartFile file,@RequestParam(defaultValue="600") double cabinetDepthMm,@RequestParam(required=false) String plan)throws Exception{
  if(file.isEmpty()||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase().endsWith(".dxf"))return ResponseEntity.badRequest().body("请上传DXF文件".getBytes(StandardCharsets.UTF_8));
  HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.parseMediaType("application/dxf"));h.setContentDisposition(ContentDisposition.attachment().filename("designed-kitchen.dxf").build());return ResponseEntity.ok().headers(h).body(service.design(file.getBytes(),cabinetDepthMm,plan));
 }
}
