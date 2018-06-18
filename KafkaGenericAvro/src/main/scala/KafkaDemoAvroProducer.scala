package com.barber.kafka.avro

import java.util.{Properties, UUID}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import com.barber.kafka.avro.models.User
import org.apache.avro.io._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.io.Source

class KafkaDemoAvroProducer(val topic:String) {

  private val props = new Properties()

  props.put("metadata.broker.list", "localhost:9092")
  props.put("message.send.max.retries", "5")
  props.put("request.required.acks", "-1")
  props.put("serializer.class", "kafka.serializer.DefaultEncoder")
  props.put("client.id", UUID.randomUUID().toString())

  private val producer =   new KafkaProducer[String,Array[Byte]](props)

  //Read avro schema file and send out
  val schema: Schema = new Parser().parse(Source.fromURL(getClass.getResource("/userSchema.avsc")).mkString)

  def send(users: List[User]): Unit = {
    val genericUser: GenericRecord = new GenericData.Record(schema)
    try {
      users.map(user => {
          // Create avro generic record object
          //Put data in that generic record object
          genericUser.put("id", user.id)
          genericUser.put("name", user.name)
          genericUser.put("email", user.email.orNull)

          // Serialize generic record object into byte array
          val writer = new SpecificDatumWriter[GenericRecord](schema)
          val out = new ByteArrayOutputStream()
          val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
          writer.write(genericUser, encoder)
          encoder.flush()
          out.close()

          val serializedBytes: Array[Byte] = out.toByteArray()
          producer.send(new ProducerRecord[String, Array[Byte]](topic, serializedBytes))
        }
      )
    } catch {
      case ex: Exception =>
        println(ex.printStackTrace().toString)
        ex.printStackTrace()
    }
  }

}