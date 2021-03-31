#!usr/bin/perl -w
=head1 Version
Author: Zeng Liang,zengliang@biozeron.com
Date: 2019-01-24
=head1 Usage
-type       <int>       1:cut head fa ; 2: cut tail fa
-len        <int>       cut fa length ; work for format 1 , 6; default : 500
-N          <int>       join N;  work for format 4 ; default : 200
-win        <int>       gc rate window size;only work for -format 8 ;default: 100000;
-sysn       <int>       fa2fq quality ; defaule F;
-scafid     <str>       scaffold prefix id; work for format 2 
-index      <str>       index seq; default: AATTC
-rate       <float>     missing rate 0-1;only work for -format 20,default 0.8
-remove     <int>       1: print cut fa; 2: remover cut fa
-format     <int>       1: only for fa ; 
                        2: chage fa name;if one input ,change as scacfNO. sort scaf len; 
                        3: compute fasta length
                        4: join fasta with some N
                        5: extract region seq
                        6: filter fasta len
                        7: remove repeat fa id 
                        8: gc rate in windows
                        9: reverse faa
                        10: index target seq
                        11: split fa , use -len para ;default 500, if len = 1,prefix = scaf id
                        12: scaf to contig
                        13: fa to fq 
                        14: fa basic stat
                        15: normalize fa;  seq len:60
                        16: atcg change to N
                        17: change scaf order, 
                        18: fa to phy format, for tree 
                        19: clw fa to axt ,for kaks compute
                        20: clw fa pos filter missing
-help                   output help information to screen
=head1 Exmple
perl deal_fa.pl fa/fa.gz map.txt -format 2
perl deal_fa.pl fa/fa.gz -scafid scaf -format 2
perl deal_fa.pl fa/fa.gz link.txt -format 4
perl deal_fa.pl fa/fa.gz region.txt -format 5
perl deal_fa.pl fa/fa.gz -format 6 -len 500 > filter.fa 
perl deal_fa.pl fa/fa.gz -format 8 -win 100000 >ref.100k.gc.txt
perl deal_fa.pl fa/fa.gz -format 9 > reverse.fa
perl deal_fa.pl fa/fa.gz -format 10 -index index-seq > 
perl deal_fa.pl fa/fa.gz prefix -format 11 -len 500 
perl deal_fa.pl fa/fa.gz -format 12 >contig.fa
perl deal_fa.pl fa/fa.gz -format 13 >fq
perl deal_fa.pl fa/fa.gz -format 13 -sysn ! > pacbio.fq
perl deal_fa.pl fa/fa.gz new.scaf.order -format 17 > new.scaf.fa
perl deal_fa.pl fa/fa.gz -format 18 >xx.phy
perl deal_fa.pl fa/fa.gz -format 20 -rate 0.5 >fa
mat.txt
id1	new_id1
id2	new_id2
...
link.txt
scaf-a	+
scaf-c  -
scaf-b  +
...
or
scaf-a
scaf-c
scaf-b
...
region.txt
Chr1	100	300
Chr2	500	900
...
=cut
use strict;use Getopt::Long;use FindBin qw($Bin $Script);       use POSIX;
my ($type, $format, $help, $len, $remove, $N, $win, $index, $sysn, $rateee, $scafid);
GetOptions(
	"type:i"=>\$type,
	"format:i"=>\$format,
	"len:i"=>\$len,
	"N:i"=>\$N,
	"scafid:s"=>\$scafid,
	"sysn:s"=>\$sysn,
	"index:s"=>\$index,
	"win:i"=>\$win,
	"remove:i"=>\$remove,
        "rate:f"=>\$rateee,
	"help"=>\$help
);
die `pod2text $0` if (@ARGV < 1 ||$help);
if($ARGV[0]=~/.gz$/){    open A,"gzip -dc $ARGV[0] |";	}
else{			 open A,$ARGV[0];		}
my %tar;	my %map;	$N    ||=200;	$len	||=500;	$win	||=100000; $index ||="AATTC";	$sysn ||="F"; $rateee ||=0.8;
$scafid ||="scaf";
if($format == 1){
	$/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/;	my $seq=join "",@a[1..$#a];	my $part;	my $size=length $seq;
		if($type == 1 && $remove == 1){			$part=substr($seq,0,$len);		}
		if($type == 1 && $remove == 2){			$part=substr($seq,$len);		}
		if($type == 2 && $remove == 1){			$part=substr($seq,-$len);		}
		if($type == 2 && $remove == 2){
			my $last=$size-$len;		$part=substr($seq,0,$last);
		}
		print ">$a[0]\n$part\n";
	}
	close A;
}
if($format == 2){
	if(defined $ARGV[1]){
		open B,$ARGV[1];
		while(<B>){
			chomp;my @a=split /\t/;	$map{$a[0]}=$a[1];
		}
		$/=">";<A>;
		while(<A>){
			chomp;my @a=split /\n/; my $seq=join "\n",@a[1..$#a]; my $size=length $seq;	my @b=split /\s+/,$a[0];
			if(exists $map{$b[0]}){
				$tar{$map{$b[0]}}{$size}=$seq;
			}
		}
		foreach my $k1(sort keys %tar){
			foreach my $k2(sort {$b<=>$a} keys %{$tar{$k1}}){
				print ">$k1\n$tar{$k1}{$k2}\n";	last;
			}
		}
	}
	else{
		$/=">";<A>; 
		while(<A>){
			chomp;my @a=split /\n/; my $seq=join "\n",@a[1..$#a]; my $size=length $seq;     my @b=split /\s+/,$a[0];
			$tar{$size}{$b[0]}=$seq;
		}
		my $num=0;
		foreach my $k1(sort {$b<=>$a} keys %tar){
			foreach my $k2(sort keys %{$tar{$k1}}){
				$num++; my $id2="$scafid$num";
				print ">$id2\n$tar{$k1}{$k2}\n"; 
			}
		}
	}
}
if($format == 3){
	$/=">";<A>;	
	#print "Chr\ttotal-length\tN-length\tno-N-length\tGC_number\n";
	while(<A>) {
		chomp ;		my @inf=split /\n/ ;
		my $chr=(split(/\s+/,$inf[0]))[0];		my $line=join("", @inf[1..$#inf]);
		@inf=();		my $Sumleng=length($line);		$line=uc($line);
		my $GC=0+($line=~s/G/G/g);	$GC+=(0+($line=~s/C/C/g));
		my $Nleng=0+($line=~s/N/0/g);
		$Nleng||=0 ; $GC||=0 ; $Sumleng||=0;
		print $chr,"\t$Sumleng\t$Nleng\t", $Sumleng-$Nleng,"\t$GC\n";
	}
}
if($format == 4){
	open B,$ARGV[1];	$/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my @b=split /\s+/,$a[0];   $tar{$b[0]}=$seq;
	}
	$/="\n"; my $n_seq="N"x$N;	my $virtual;	my $head=<B>;	chomp $head;	my @head=split /\s+/,$head;
	if(@head > 1){
		if($head[1] eq "+"){
			$virtual=$tar{$head[0]};
		}
		else{
			my $pp=$tar{$head[0]};	$pp=~tr/[ATCG]/[TAGC]/;	$pp=reverse $pp;	$virtual=$pp;
		}
	}
	else{
		$virtual=$tar{$head[0]};
	}
	while(<B>){
		chomp;	my @a=split;	
		$virtual.=$n_seq;
		if(@a > 1){
			if($a[1] eq "+"){
				$virtual.=$tar{$a[0]};
			}
			else{
				my $pp=$tar{$a[0]};  $pp=~tr/[ATCG]/[TAGC]/; $pp=reverse $pp;        $virtual.=$pp;
			}
		}
		else{	
			$virtual.=$tar{$a[0]};
		}
	}
	print ">virtual\n$virtual\n";
}
if($format == 5){
	open B,$ARGV[1];        $/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my @b=split /\s+/,$a[0];   $tar{$b[0]}=$seq;
	}
	$/="\n";
	while(<B>){
		chomp;  my @a=split;	my $size=$a[2]-$a[1]+1;	my $pos=$a[1]-1;	my $iddd=join "_",@a;
		my $subseq=substr($tar{$a[0]},$pos,$size);	
		print ">$iddd\n$subseq\n";
	}
}
if($format >= 6 && $format <= 10){
	$/=">";<A>;	my %id;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my @b=split /\s+/,$a[0];  my  $seq_len=length $seq;
		$seq=uc($seq);
		if($format == 6){
			next if($seq_len < $len);	print ">$b[0]\n$seq\n";
		}
		if($format == 7){
			if(!exists $id{$b[0]}){
				print ">$b[0]\n$seq\n";	$id{$b[0]}=1;
			}
		}
		if($format == 8){
			my $len=length $seq;   my $step=int($seq_len/$win);
			foreach my $k(0..$step){
				my $pos=$k*$win;my $tar_seq=substr($seq,$pos,$win);
				my $GC=0+($tar_seq=~s/G/G/g);      $GC+=(0+($tar_seq=~s/C/C/g));
				my $sub_len=length $tar_seq;	   my $rate=$GC/$sub_len;
				my $sub_size=$k*$win;my $sub_end=$sub_size+$win-1;
				print "$b[0]\t$sub_size\t$sub_end\t$rate\n";
			}	
		}
		if($format == 9){
			$seq=~tr/[ATCG]/[TAGC]/; $seq=reverse $seq;		print ">$b[0]\n$seq\n";
		}
		if($format == 10){
			my $site=1;
			while(1){
				my $pp=index($seq,$index,$site);
				last if($pp < 0);	print "$b[0]\t$pp\t$index\n";	$site=$pp+1;
			}
		}		
	}
}
if($format == 11){
	my $rec=0;my $num=1;	$/=">";<A>;
	if($len > 1){
		open OUT,">$ARGV[1]$num.fa";
		while(<A>){
			chomp;$rec++;	print OUT ">$_";
			if($rec >= $len){
				close OUT;
				$rec=0; $num++;
				open OUT,">$ARGV[1]$num.fa";
			}
		}
		close A;close OUT;
	}
	else{
		while(<A>){
			chomp;my @a=split /\n/;	my @b=split /\s+/,$a[0];
			open OUT,">$b[0].fa";
			print OUT ">$_";
			close OUT;
		}
		close A;
	}
}
if($format == 12){
	$/=">";<A>;	my %tar12;	my $tar12=0;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; 	my @c=split /N+/,$seq;
		foreach my $k(@c){
			my $len=length $k;	next if($len < 200);	$tar12++;			$tar12{$len}{$tar12}=$k;
		}
	}
	close A;my $new_tar12=0;
	foreach my $k1(sort {$b<=>$a} keys %tar12){
		foreach my $k2(sort {$a<=>$b} keys %{$tar12{$k1}}){
			$new_tar12++;
			print ">Contig$new_tar12\n$tar12{$k1}{$k2}\n";
		}
	}
}
if($format == 13){
	$/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my $len=length $seq;
		my $qual="$sysn"x$len;		print "\@$a[0]\n$seq\n+\n$qual\n";
	}
	close A;
}
if($format == 14){
	$/=">";<A>; my $large=0;	my $gc=0;	my $small=10000000000000000000000;my $num=0;
	my $n50=0;	my $l50=0;	my $n90=0;  my $l90=0;		my %seq;	my $total=0;my $GC=0;my $nn=0;
	my $large_num=0;	my $large_base=0;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my $len=length $seq;	my @b=split /\s+/,$a[0];
		$seq{$b[0]}=$len;	$total+=$len;	$num++;
		if($len >= 1000){
			$large_num++;	$large_base+=$len;
		}
		$GC+=0+($seq=~s/G/G/g);  $GC+=0+($seq=~s/C/C/g);
		$GC+=0+($seq=~s/g/g/g);  $GC+=0+($seq=~s/c/c/g);
		$nn+=0+($seq=~s/N/N/g);	 $nn+=0+($seq=~s/n/n/g);
		if($len > $large){				$large=$len;			}
		if($len < $small){				$small=$len;			}

	}
	my $acc1=0;	my $acc2=0;
	foreach my $k(sort {$seq{$b}<=>$seq{$a}} keys %seq){
		$acc1+=$seq{$k};	
		if($acc1/$total >= 0.5){
			$n50=$seq{$k};	$l50=$k;	last;
		}
	}
	foreach my $k(sort {$seq{$b}<=>$seq{$a}} keys %seq){
		$acc2+=$seq{$k};
		if($acc2/$total >= 0.9){
			$n90=$seq{$k};  $l90=$k;    last;
		}
	}
	close A;my $gc_rate=$GC/$total;	my $nn_rate=$nn/$total;	my $aver=$total/$num;	
	print "Total length:\t$total\nTotal number:\t$num\nAver len:\t$aver\nGC rate:\t$gc_rate\nmax length:\t$large\n";
	print "large scaffords No (>=1k):\t$large_num\nlarge scaffords base (>=1k):\t$large_base\n";
	print "min length:\t$small\nN50:\t$n50\nL50:\t$l50\nN90:\t$n90\nL90:\t$l90\nN rate:\t$nn_rate\n";
}
if($format == 15){
	$/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/; my $seq=join "",@a[1..$#a]; my $len=length $seq;	$seq=uc($seq);
		print ">$a[0]\n";
		for(my $i=0; $i < $len; $i+=60){
			my $part=substr($seq,$i,60);
			print "$part\n";
		}
	}
	close A;
}
if($format == 16){
	while(<A>){		
		if($_=~/^>/){
			print "$_";
		}
		else{
			$_=~s/[atcg]/N/g;
			print "$_";
		}
	}
	close A;
}
if($format == 17){
	$/=">";<A>;
	while(<A>){
		chomp;my @a=split /\n/; my @b=split /\s+/,$a[0];	$map{$b[0]}=$_;
	}
	$/="\n";open B,$ARGV[1];
	while(<B>){
		chomp;my @a=split /\t/; print ">$map{$a[0]}";
	}
}
if($format == 18){
	$/=">";<A>;
	my %h;  my $num=0;  my $len=0;
	while(<A>){
		chomp;my @a=split /\n+/;my $seq=join "",@a[1..$#a];
		$h{$a[0]}=$seq; $len=length $seq;
		$num++;
	}
	close A;
	$/="\n";
	printf "%5d   %d\n",$num,$len;
	foreach my $k(sort keys %h){
		printf "%-29s",$k;
		print "$h{$k}\n";
	}
}
if($format == 19){
        $/=">";<A>;
	print   "$ARGV[0]\n";
        while(<A>){
                chomp;my @a=split /\n/; my @b=split /\s+/,$a[0];      my $seq=join "",@a[1..$#a];
		print 	"$seq\n";
        }
	print "\n";
}
if($format == 20){
        $/=">";<A>;	my %part20;	my $num20=0;	my %last20;
        while(<A>){
                chomp;my @a=split /\n/; my @b=split /\s+/,$a[0];      my $seq=join "",@a[1..$#a];	$num20++;	my @seq=split //,$seq;
		foreach my $k(0..$#seq){
			if($seq[$k] eq "-"){
				$part20{$k}++;
			}
		}
        }
	close A;
	foreach my $k(sort keys %part20){
		if($part20{$k}/$num20 <= $rateee){
			$last20{$k}=1;
		}
	}	
	open A,$ARGV[0]; <A>;
	while(<A>){
		chomp;my @a=split /\n/; my @b=split /\s+/,$a[0];      my $seq=join "",@a[1..$#a];	my @seq=split //,$seq;
		print ">$b[0]\n";
		foreach my $k(0..$#seq){
			if(exists $last20{$k}){
				print "$seq[$k]";
			}
		}
		print "\n";
	}	
}

