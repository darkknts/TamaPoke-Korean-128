#include "Preferences.h"
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <fstream>
namespace { std::string storageDir; }
void Preferences::setStorageDirectory(const char* p) { storageDir = p ? p : ""; }
std::string Preferences::filePath() const { return storageDir + "/tamapoke_" + ns_ + ".prefs"; }
bool Preferences::load() {
 values_.clear(); std::ifstream in(filePath(),std::ios::binary); if(!in) return true;
 uint32_t count=0; in.read(reinterpret_cast<char*>(&count),sizeof(count)); if(!in||count>4096) return false;
 for(uint32_t i=0;i<count;i++){ uint16_t ks=0; uint32_t vs=0; in.read(reinterpret_cast<char*>(&ks),sizeof(ks)); if(!in||!ks||ks>1024)return false;
 std::string k(ks,'\\0'); in.read(k.data(),ks); in.read(reinterpret_cast<char*>(&vs),sizeof(vs)); if(!in||vs>1048576)return false;
 std::vector<uint8_t> v(vs); if(vs)in.read(reinterpret_cast<char*>(v.data()),vs); if(!in)return false; values_[k]=std::move(v); } return true;
}
bool Preferences::persist() const {
 auto path=filePath(), tmp=path+".tmp"; std::ofstream out(tmp,std::ios::binary|std::ios::trunc); if(!out)return false;
 uint32_t count=values_.size(); out.write(reinterpret_cast<const char*>(&count),sizeof(count));
 for(const auto& x:values_){ uint16_t ks=x.first.size(); uint32_t vs=x.second.size(); out.write(reinterpret_cast<const char*>(&ks),sizeof(ks)); out.write(x.first.data(),ks); out.write(reinterpret_cast<const char*>(&vs),sizeof(vs)); if(vs)out.write(reinterpret_cast<const char*>(x.second.data()),vs); }
 out.close(); if(!out)return false; return std::rename(tmp.c_str(),path.c_str())==0;
}
bool Preferences::begin(const char* n,bool r){ns_=n?n:"default";ro_=r;open_=load();return open_;}
void Preferences::end(){open_=false;values_.clear();}
bool Preferences::clear(){if(ro_)return false;values_.clear();return persist();}
bool Preferences::isKey(const char* k){return values_.find(k?k:"")!=values_.end();}
bool Preferences::remove(const char* k){if(ro_)return false;values_.erase(k?k:"");return persist();}
size_t Preferences::putBytes(const char* k,const void* d,size_t n){if(ro_||!k||(!d&&n))return 0;auto p=static_cast<const uint8_t*>(d);values_[k]=n?std::vector<uint8_t>(p,p+n):std::vector<uint8_t>();return persist()?n:0;}
size_t Preferences::getBytes(const char* k,void* d,size_t n){auto i=values_.find(k?k:"");if(i==values_.end()||!d)return 0;auto z=std::min(n,i->second.size());if(z)std::memcpy(d,i->second.data(),z);return z;}
template<typename T> size_t Preferences::putNumber(const char* k,T v){return putBytes(k,&v,sizeof(v));}
template<typename T> T Preferences::getNumber(const char* k,T d){auto i=values_.find(k?k:"");if(i==values_.end()||i->second.size()!=sizeof(T))return d;T v{};std::memcpy(&v,i->second.data(),sizeof(v));return v;}
size_t Preferences::putBool(const char*k,bool v){return putNumber(k,static_cast<uint8_t>(v));}
size_t Preferences::putUChar(const char*k,uint8_t v){return putNumber(k,v);}
size_t Preferences::putChar(const char*k,int8_t v){return putNumber(k,v);}
size_t Preferences::putShort(const char*k,int16_t v){return putNumber(k,v);}
size_t Preferences::putUShort(const char*k,uint16_t v){return putNumber(k,v);}
size_t Preferences::putInt(const char*k,int32_t v){return putNumber(k,v);}
size_t Preferences::putUInt(const char*k,uint32_t v){return putNumber(k,v);}
size_t Preferences::putString(const char*k,const char*v){return v?putBytes(k,v,std::strlen(v)+1):0;}
bool Preferences::getBool(const char*k,bool d){return getNumber<uint8_t>(k,d?1:0)!=0;}
uint8_t Preferences::getUChar(const char*k,uint8_t d){return getNumber(k,d);}
int8_t Preferences::getChar(const char*k,int8_t d){return getNumber(k,d);}
int16_t Preferences::getShort(const char*k,int16_t d){return getNumber(k,d);}
uint16_t Preferences::getUShort(const char*k,uint16_t d){return getNumber(k,d);}
int32_t Preferences::getInt(const char*k,int32_t d){return getNumber(k,d);}
uint32_t Preferences::getUInt(const char*k,uint32_t d){return getNumber(k,d);}
size_t Preferences::getString(const char*k,char*out,size_t n){if(!out||!n)return 0;auto i=values_.find(k?k:"");if(i==values_.end()){out[0]=0;return 0;}auto z=std::min(n-1,i->second.size());if(z)std::memcpy(out,i->second.data(),z);out[z]=0;return z;}
