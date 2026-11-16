#include "Arduino.h"
#include "Preferences.h"
#include <windows.h>
#include <shlobj.h>
#include <map>
#include <vector>
#include <fstream>

DesktopSerial Serial;
uint32_t millis() { return GetTickCount(); }
long random(long upper) { if (upper <= 0) return 0; return std::rand() % upper; }
long random(long lower, long upper) { return upper <= lower ? lower : lower + random(upper-lower); }

using Values = std::map<std::string, std::vector<unsigned char>>;
static std::map<std::string, Values> store;
static bool loaded = false;
static std::string savePath() {
  wchar_t path[MAX_PATH]{};
  if (SUCCEEDED(SHGetFolderPathW(nullptr, CSIDL_LOCAL_APPDATA, nullptr, SHGFP_TYPE_CURRENT, path))) {
    std::wstring p(path); p += L"\\TamaPoke";
    CreateDirectoryW(p.c_str(), nullptr); p += L"\\save.dat";
    return std::string(p.begin(), p.end());
  }
  return "tamapoke-save.dat";
}
static void loadStore() {
  if (loaded) return;
  loaded = true;
  std::ifstream in(savePath(), std::ios::binary);
  if (!in) return;
  uint32_t groups = 0; in.read((char*)&groups, 4); if (!in || groups > 20) return;
  for (uint32_t i=0; i<groups; ++i) {
    uint16_t nl=0; in.read((char*)&nl,2); if (!in || nl>128) return;
    std::string ns(nl, '\0'); in.read(&ns[0],nl);
    uint32_t count=0; in.read((char*)&count,4); if (!in || count>500) return;
    for (uint32_t k=0; k<count; ++k) {
      uint16_t kl=0; uint32_t vl=0;
      in.read((char*)&kl,2); in.read((char*)&vl,4); if (!in || kl>128 || vl>65536) return;
      std::string key(kl, '\0'); in.read(&key[0],kl);
      std::vector<unsigned char> value(vl); if (vl) in.read((char*)value.data(),vl);
      if (!in) return;
      store[ns][key]=std::move(value);
    }
  }
}
static void persist() {
  std::ofstream out(savePath(), std::ios::binary|std::ios::trunc);
  uint32_t groups=(uint32_t)store.size(); out.write((char*)&groups,4);
  for (const auto& g:store) {
    uint16_t nl=(uint16_t)g.first.size(); out.write((char*)&nl,2); out.write(g.first.data(),nl);
    uint32_t count=(uint32_t)g.second.size(); out.write((char*)&count,4);
    for (const auto& kv:g.second) {
      uint16_t kl=(uint16_t)kv.first.size(); uint32_t vl=(uint32_t)kv.second.size();
      out.write((char*)&kl,2); out.write((char*)&vl,4); out.write(kv.first.data(),kl);
      if (vl) out.write((const char*)kv.second.data(),vl);
    }
  }
}
bool Preferences::begin(const char*n,bool ro){loadStore();ns_=n;ro_=ro;open_=true;return true;}
void Preferences::end(){open_=false;}
bool Preferences::clear(){if(!open_||ro_)return false;store[ns_].clear();persist();return true;}
bool Preferences::isKey(const char*k){return open_&&store[ns_].count(k);}
bool Preferences::remove(const char*k){if(!open_||ro_)return false;auto n=store[ns_].erase(k);persist();return n>0;}
static size_t putRaw(const std::string&ns,bool open,bool ro,const char*k,const void*v,size_t n){
  if(!open||ro)return 0;const auto*b=(const unsigned char*)v;
  store[ns][k]=std::vector<unsigned char>(b,b+n);persist();return n;
}
#define PUT_SCALAR(method,type) size_t Preferences::method(const char*k,type v){return putRaw(ns_,open_,ro_,k,&v,sizeof(v));}
PUT_SCALAR(putBool,bool) PUT_SCALAR(putUChar,uint8_t) PUT_SCALAR(putChar,int8_t)
PUT_SCALAR(putShort,int16_t) PUT_SCALAR(putUShort,uint16_t) PUT_SCALAR(putInt,int32_t) PUT_SCALAR(putUInt,uint32_t)
size_t Preferences::putBytes(const char*k,const void*v,size_t n){return putRaw(ns_,open_,ro_,k,v,n);}
size_t Preferences::putString(const char*k,const char*v){return putBytes(k,v,strlen(v)+1);}
template<class T> static T getVal(const std::string&ns,bool open,const char*k,T d){
  if(!open)return d;auto i=store[ns].find(k);if(i==store[ns].end()||i->second.size()!=sizeof(T))return d;
  T v;memcpy(&v,i->second.data(),sizeof(T));return v;
}
bool Preferences::getBool(const char*k,bool d){return getVal(ns_,open_,k,d);}
uint8_t Preferences::getUChar(const char*k,uint8_t d){return getVal(ns_,open_,k,d);}
int8_t Preferences::getChar(const char*k,int8_t d){return getVal(ns_,open_,k,d);}
int16_t Preferences::getShort(const char*k,int16_t d){return getVal(ns_,open_,k,d);}
uint16_t Preferences::getUShort(const char*k,uint16_t d){return getVal(ns_,open_,k,d);}
int32_t Preferences::getInt(const char*k,int32_t d){return getVal(ns_,open_,k,d);}
uint32_t Preferences::getUInt(const char*k,uint32_t d){return getVal(ns_,open_,k,d);}
size_t Preferences::getBytes(const char*k,void*b,size_t n){
  if(!open_)return 0;auto i=store[ns_].find(k);if(i==store[ns_].end())return 0;
  size_t z=std::min(n,i->second.size());memcpy(b,i->second.data(),z);return z;
}
size_t Preferences::getString(const char*k,char*b,size_t n){
  if(!open_||!n)return 0;auto i=store[ns_].find(k);if(i==store[ns_].end()){b[0]=0;return 0;}
  size_t z=std::min(n-1,i->second.size());memcpy(b,i->second.data(),z);b[z]=0;return strlen(b);
}
