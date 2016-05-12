package com.btcontract.wallet.lightning.thundercloud

import java.math.BigInteger
import org.bitcoinj.core.Utils.HEX
import org.spongycastle.math.ec.ECPoint
import com.btcontract.wallet.Utils.Bytes
import org.bitcoinj.core.{Sha256Hash, ECKey}
import spray.json._


object ThundercloudProtocol extends DefaultJsonProtocol { me =>
  implicit object BigIntegerFormat extends JsonFormat[BigInteger] {
    def write(bigInteger: BigInteger) = JsString apply bigInteger.toString
    def read(json: JsValue) = new BigInteger(me jsonToString json)
  }

  implicit object ECPointJson extends JsonFormat[ECPoint] {
    def write(point: ECPoint) = JsString apply HEX.encode(point getEncoded true)
    def read(json: JsValue) = ECKey.CURVE.getCurve decodePoint HEX.decode(me jsonToString json)
  }

  def jsonToString(json: JsValue) = json.convertTo[String]
  implicit val blindParamsFmt = jsonFormat[ECPoint, BigInteger, BigInteger,
    BigInteger, BigInteger, BlindParams](BlindParams, "key", "a", "b", "c", "bInv")

  // Request and Response
  implicit val requestFmt = jsonFormat[Long, Bytes, String, String, Request](Request, "mSatAmount", "ephemeral", "message", "id")
  implicit val responseFmt = jsonFormat[Request, Bytes, Response](Response, "request", "lnRouteData")

  // Message and Wrap
  implicit val messageFmt = jsonFormat[Bytes, Bytes, Message](Message, "pubKey", "content")
  implicit val wrapFmt = jsonFormat[Message, Long, Wrap](Wrap, "data", "stamp")

  // User signed email and server signed email
  implicit val smFmt = jsonFormat[String, String, String, SignedMail](SignedMail, "email", "pubKey", "signature")
  implicit val ssmFmt = jsonFormat[SignedMail, String, ServerSignedMail](ServerSignedMail, "client", "signature")
}

// This is a "response-to" ephemeral key, it's private part
// should be stored in a database because my bloom filter has it's mask
case class Request(mSatAmount: Long, ephemeral: Bytes, message: String, id: String)
case class Response(request: Request, lnRouteData: Bytes)

// Request/Response container and Wrap
case class Message(pubKey: Bytes, content: Bytes)
case class Wrap(data: Message, stamp: Long)

// Client and server signed email to key mappings
case class ServerSignedMail(client: SignedMail, signature: String)
case class SignedMail(email: String, pubKey: String, signature: String) {
  def dataHash = Sha256Hash.of(email + pubKey + signature getBytes "UTF-8")
  def pubKeyClass = ECKey.fromPublicOnly(HEX decode pubKey)
  def emailHash = Sha256Hash.of(email getBytes "UTF-8")
}