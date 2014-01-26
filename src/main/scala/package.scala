/*     
**                  _______        
**                 |__   __|     reqT API  
**   _ __  ___   __ _ | |        (c) 2011-2014, Lund University  
**  |  __|/ _ \ / _  || |        http://reqT.org
**  | |  |  __/| (_| || |   
**  |_|   \___| \__  ||_|   
**                 | |      
**                 |_|      
** reqT is open source, licensed under the BSD 2-clause license: 
** http://opensource.org/licenses/bsd-license.php 
*****************************************************************/

package object reqT extends Init with Licence {
  import scala.language.implicitConversions

  implicit class ElemSeqToModel(seq: Seq[Elem]) {
    def toModel = Model(seq:_*)
  }

  def uuid = java.util.UUID.randomUUID.toString


  
}
