#include "xml_marshaller.h"
using namespace xtfs3;


XMLMarshaller::XMLMarshaller()
{
  buffer = new yidl::StringBuffer( "<?xml version=\"1.0\"?>", 21 );
  writer = genxNew( NULL, NULL, this );
	sender.send = genx_send;
	sender.sendBounded = genx_sendBounded;
	sender.flush = genx_flush;
	genxStartDocSender( writer, &sender );	
}

XMLMarshaller::XMLMarshaller( genxWriter writer, const char* sequence_key )
  : writer( writer )
{
  if ( sequence_key != NULL )
    this->sequence_key = sequence_key;
}

XMLMarshaller::~XMLMarshaller()
{ 
  if ( buffer != NULL )
    genxDispose( writer );
}

genxStatus XMLMarshaller::genx_flush( void* ) 
{ 
  return GENX_SUCCESS; 
}

genxStatus XMLMarshaller::genx_send( void* userData, constUtf8 data ) 
{ 
  XMLMarshaller* this_ = static_cast<XMLMarshaller*>( userData );
  this_->buffer->put( reinterpret_cast<const char*>( data ), std::strlen( reinterpret_cast<const char*>( data ) ) );
  return GENX_SUCCESS; 
}

genxStatus XMLMarshaller::genx_sendBounded( void* userData, constUtf8 start, constUtf8 end ) 
{ 
  XMLMarshaller* this_ = static_cast<XMLMarshaller*>( userData );
  this_->buffer->put( reinterpret_cast<const char*>( start ), end - start );
  return GENX_SUCCESS; 
}

yidl::auto_Buffer XMLMarshaller::get_buffer() const 
{ 
  genxEndDocument( writer );
  return buffer->incRef(); 
}

void XMLMarshaller::writeBoolean( const char* key, uint32_t, bool value )
{
	writeElementStart( key );
	genxAddText( writer, ( constUtf8 )( value ? "1" : "0" ) );
	writeElementEnd();
}

void XMLMarshaller::writeBuffer( const char* key, uint32_t tag, yidl::auto_Buffer value )
{
  writeString( key, tag, static_cast<char*>( *value ), value->size() );
}

void XMLMarshaller::writeDouble( const char* key, uint32_t, double value )
{
  writeNumeric( key, value, "%.8f" );
}

void XMLMarshaller::writeElementStart( const char* key )
{
	genxStartElementLiteral( writer, NULL, ( constUtf8 )( !sequence_key.empty() ? sequence_key.c_str() : key ) );
}

void XMLMarshaller::writeElementEnd()
{
	genxEndElement( writer );
}

void XMLMarshaller::writeInt64( const char* key, uint32_t, int64_t value )
{
  writeNumeric( key, value, "%lld" );
}

void XMLMarshaller::writeMap( const char* key, uint32_t, const yidl::Map& value )
{
	writeElementStart( key ); 
  XMLMarshaller child_xml_marshaller( writer );
  value.marshal( child_xml_marshaller );
	writeElementEnd();
}

template <class NumericType>
void XMLMarshaller::writeNumeric( const char* key, NumericType value, const char* printf_format )
{
  char value_str[32];
	writeElementStart( key );
#ifdef _WIN32
	genxAddCountedText( writer, ( constUtf8 )value_str, sprintf_s( value_str, printf_format, value ) );
#else
	genxAddCountedText( writer, ( constUtf8 )value_str, sprintf( value_str, printf_format, value ) );
#endif
	writeElementEnd();
}

void XMLMarshaller::writeSequence( const char* key, uint32_t, const yidl::Sequence& value )
{
   XMLMarshaller child_xml_marshaller( writer, key );
   value.marshal( child_xml_marshaller );
}

void XMLMarshaller::writeString( const char* key, uint32_t, const char* value, size_t value_len )
{
	writeElementStart( key );
	genxAddCountedText( writer, ( constUtf8 )value, value_len );
	writeElementEnd();
}

void XMLMarshaller::writeStruct( const char* key, uint32_t, const yidl::Struct& value )
{
	writeElementStart( key ); 
  XMLMarshaller child_xml_marshaller( writer );
  value.marshal( child_xml_marshaller );
	writeElementEnd();
}

void XMLMarshaller::writeUint64( const char* key, uint32_t, uint64_t value )
{
  writeNumeric( key, value, "%llu" );
}
