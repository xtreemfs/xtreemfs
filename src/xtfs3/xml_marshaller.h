#ifndef _XTFS3_XML_MARSHALLER_H_
#define _XTFS3_XML_MARSHALLER_H_

#include "yield.h"

#include "genx.h"


namespace xtfs3
{
  class XMLMarshaller : public yidl::Marshaller
  {
  public:
    XMLMarshaller();
    ~XMLMarshaller();

    yidl::auto_Buffer get_buffer() const;

    YIDL_MARSHALLER_PROTOTYPES;
    void writeUint64( const char* key, uint32_t tag, uint64_t value );

  private:
    XMLMarshaller( genxWriter writer, const char* sequence_key = NULL );
    
    yidl::auto_StringBuffer buffer;    
  	genxSender sender;
    std::string sequence_key;
    genxWriter writer;

    static genxStatus genx_flush( void* );
    static genxStatus genx_send( void*, constUtf8 data );
    static genxStatus genx_sendBounded( void*, constUtf8 start, constUtf8 end );
    void writeElementStart( const char* key );
    void writeElementEnd();
		template <class NumericType> void writeNumeric( const char* key, NumericType value, const char* printf_format );
  };
};

#endif
