package com.handyman.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import com.handyman.repository.*;
import com.handyman.model.*;
import com.handyman.dto.GalleryDtos.*;

@Service @Transactional
public class GalleryService {
 private final CategoryRepository categories;
 private final PhotoRepository photos;
 private final CloudinaryStorage storage;
 private final UploadValidator validator;
 private final StorageDiagnostics diagnostics;
 public GalleryService(CategoryRepository c,PhotoRepository p,CloudinaryStorage s,UploadValidator v,StorageDiagnostics d) { categories=c;photos=p;storage=s;validator=v;diagnostics=d; }
 private Category category(Long id) { return categories.lockById(id).orElseThrow(()->new GalleryException(404,"הקטגוריה לא נמצאה")); }
 private Photo photo(Long id) { return photos.findById(id).orElseThrow(()->new GalleryException(404,"התמונה לא נמצאה")); }
 private CategoryView view(Category c) { return new CategoryView(c.getId(),c.getName(),c.getSlug(),c.getSortOrder(),c.getCreatedAt()); }
 private PhotoView view(Photo p) { return new PhotoView(p.getId(),storage.url(p.getCloudinaryPublicId(),false),storage.url(p.getCloudinaryPublicId(),true),p.getCaption()); }
 @Transactional(readOnly=true) public List<CategoryView> categories() { return categories.findAllByOrderBySortOrderAscIdAsc().stream().map(this::view).toList(); }
 @Transactional(readOnly=true) public List<PhotoView> photos(String slug) {
  if(slug==null) return photos.findAllByOrderBySortOrderAscIdAsc().stream().map(this::view).toList();
  Category c=categories.findBySlug(slug).orElseThrow(()->new GalleryException(404,"הקטגוריה לא נמצאה"));
  return photos.findByCategoryIdOrderBySortOrderAscIdAsc(c.getId()).stream().map(this::view).toList();
 }
 public CategoryView create(CategoryInput input) {
  if(categories.existsBySlug(input.slug())) throw new GalleryException(409,"מזהה הקטגוריה כבר קיים");
  Category c=new Category();c.setName(input.name().trim());c.setSlug(input.slug());
  c.setSortOrder(categories.findAll().stream().mapToInt(x->x.getSortOrder()).max().orElse(-1)+1);
  return view(categories.saveAndFlush(c));
 }
 public CategoryView rename(Long id,Rename input) { Category c=category(id);c.setName(input.name().trim());return view(c); }
 public void deleteCategory(Long id) {
  Category c=category(id);
  if(photos.existsByCategoryId(id)) throw new GalleryException(409,"לא ניתן למחוק קטגוריה המכילה תמונות");
  categories.delete(c);categories.flush();
 }
 private void orderCheck(List<Long> ids,List<Long> existing) {
  if(ids.size()!=new HashSet<>(ids).size() || !new HashSet<>(ids).equals(new HashSet<>(existing))) throw new GalleryException(400,"יש לשלוח את כל המזהים ללא כפילויות");
 }
 public void orderCategories(List<Long> ids) {
  var all=categories.findAllByOrderBySortOrderAscIdAsc();orderCheck(ids,all.stream().map(c->c.getId()).toList());
  for(int i=0;i<ids.size();i++) category(ids.get(i)).setSortOrder(i);
 }
 public List<PhotoView> upload(Long categoryId,List<MultipartFile> files) {
  validator.validate(files);Category c=category(categoryId);
  List<String> uploaded=new ArrayList<>();
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
   @Override public void afterCompletion(int status) {
    if(status!=STATUS_COMMITTED) for(String id:uploaded) {
     try { storage.delete(id); } catch(RuntimeException ex) {
      diagnostics.warn("rollback cleanup",ex);
     }
    }
   }
  });
  int order=photos.findByCategoryIdOrderBySortOrderAscIdAsc(c.getId()).stream().mapToInt(p->p.getSortOrder()).max().orElse(-1)+1;
  List<PhotoView> result=new ArrayList<>();
  for(MultipartFile file:files) {
   String publicId=storage.upload(file,c.getSlug());uploaded.add(publicId);
   Photo p=new Photo();p.setCategory(c);p.setCloudinaryPublicId(publicId);p.setSortOrder(order++);
   result.add(view(photos.saveAndFlush(p)));
  }
  return result;
 }
 public PhotoView caption(Long id,Caption input) { Photo p=photo(id);category(p.getCategory().getId());p.setCaption(input.caption()==null?null:input.caption().trim());return view(p); }
 public void deletePhoto(Long id) {
  Photo p=photo(id);category(p.getCategory().getId());
  storage.delete(p.getCloudinaryPublicId());photos.delete(p);photos.flush();
 }
 public void orderPhotos(Long categoryId,List<Long> ids) {
  category(categoryId);var all=photos.findByCategoryIdOrderBySortOrderAscIdAsc(categoryId);
  orderCheck(ids,all.stream().map(p->p.getId()).toList());
  Map<Long,Photo> byId=new HashMap<>();all.forEach(p->byId.put(p.getId(),p));
  for(int i=0;i<ids.size();i++) byId.get(ids.get(i)).setSortOrder(i);
 }
}
