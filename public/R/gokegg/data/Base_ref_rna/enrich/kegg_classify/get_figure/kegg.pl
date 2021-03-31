use Try::Tiny;
use LWP::Simple;
use LWP::UserAgent;
use HTML::TreeBuilder;

my ($line,@inf,%path);
my $ua =LWP::UserAgent->new();
open IN, "kegg_table.xls" or die "can not open file: kegg_table.xls\n";
<IN>;
while($line=<IN>){
	chomp $line;    	@inf=split /\s+/,$line;     $path{$inf[0]}=1;
}
close IN;
srand();
foreach my $i (keys %path){
    #my $url="http://www.kegg.jp/pathway/".$i.$path{$i};
    my $url="http://www.kegg.jp/pathway/".$i;
	#print "$url\n";
    &savekegg($url,"$i.png","$i.html");
	#my $j = rand(5);sleep($j);
}
	#&savekegg("http://www.kegg.jp/pathway/ko00010+K01835","ko00010.png","ko00010.html");

sub savekegg(){
	my $url=shift;
	my $imgfile=shift;
	my $htmlfile=shift;
	try{
		my $response =$ua->get($url);
		my $filepath=$imgfile;
		my $html;
		if($response->is_success){
		  $html = $response->decoded_content;
		  $html=&formathtml($html);
	 	   
		   my $imgurl;
		   #try{
		   		my $datapage=HTML::TreeBuilder->new_from_content($html);		
		   		my @data=$datapage->find_by_attribute("usemap","#mapdata"); 
		   		
		   			$imgurl=$data[0]->attr("src") or return &savekegg($url,$imgfile,$htmlfile);
		   		#}catch{
		   			#warn("Parsing html error! retrying ... \n");
		  			
		   		#}
		   		$datapage->delete();
		   #}catch{
		   		 #warn("Parsing html error! retrying ... \n");
		  		 #&savekegg($url,$imgfile,$htmlfile);
		   #}
		   
		    $html=&formathtml1($html,$imgfile);
		   	 open (HTML,"> $htmlfile") or die "Can't create file $htmlfile\n";
	 		 print HTML  $html;
	 		 close HTML;
	 		 
		 #  getstore($imgurl,$filepath) or &savekegg($url,$imgfile,$htmlfile);
		 	my $r=$ua->mirror($imgurl,$filepath);
		 	unless($r->is_success||$r->code eq '304'){
		 		 warn("Saveing image file error!:".$r->status_line." retrying get from $imgurl ... \n");
		 		 return &savekegg($url,$imgfile,$htmlfile);
		 	}
		 	if($r->code eq '304'){
		 		warn("It seems to image haven't being coloring,download uncolored image form $imgurl.This problem is form kegg server.\n");
		 	}
	
		}else{
		    if($response->code eq '414' || $response->code eq '500'){
		   		warn($response->status_line."  ,server not accept or error,skiping ... \n");
		   }else{
		   		warn($response->status_line." retrying ... \n");		   
		   		#return  &savekegg($url,$imgfile,$htmlfile);
		   }
		}
	}catch{
		warn("Server connection serious error:$_,Geting pathway image from  $url  ...\n");
		#return &savekegg($url,$imgfile,$htmlfile);
	}
}

sub formathtml(){
	my($htm)=@_;
	$htm =~ s/\"\//\"http\:\/\/www.kegg.jp\//g;
	$htm =~ s/\'\//\'http\:\/\/www.kegg.jp\//g;
	return $htm;
}
sub formathtml1(){
	my $htm =shift;
	my $imgname=shift;	
	$htm =~ s/<img src\=\".*\" usemap\=\"#mapdata\" border\=\"0\" \/>/<img src\=\"$imgname\" usemap\=\"#mapdata\" border\=\"0\" \/>/g;
	return $htm;
}
