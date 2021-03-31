#!usr/bin/perl
use strict;
use warnings;
use strict;use Getopt::Long;
=head1 Version
Author: zengliang zengliang@gmail.com
Date: 2020-06-05
=head1 Usage
perl find_identical.pl file1 file2  [options]
perl find_identical.pl fq1 fq2 txt.file output:prefix -format 4 [options]
-format   <int>   the input file type, 1 two file is table ,2 first is fa second is table,
               3 two fasta file,
               4 first,second is PE fastq third is table; 
               5 two table file, add file2 to file 1,not find use -sysn;
               6 filter head sample id;
               7 two table file, site in region, file1 region; file2 site 
               8 find id in head position and print
               9 density for table, use -win for windows size
               10 split table,format group
               11 table head as new order
               12 two table overlap
-site1    <int>   the same site 1
-site2    <int>   the same site 2
-win      <int>   windows size,default : 100000
-head	  <int>	  first row read or no ? only work for format 1; 0:no defaule, 1 use for type 1,4 ; 2 use for type 2,5 ; 3 use for type 3,6
-type     <int>   you can chose 1,2,3,4,5,6;1 output the identical in file1,2 output the identical in file2,3 output the identical in the file1 and file2 report same ID;4 output the different in file1; 5 output the different in file2, for fq module type only is 1 or 4,  6 output the identical in the file1 and file2 diff ID fill 0; 
-sysn     <int>   type = 6 fill sysn: 1: _ , 2: -, 3: NA, 4: 0, default: 4;
-forward  <int>   add file site; 1: forward ; 0: backward ; default 0 ; only work for format 5
-select   <int>   0: select sample or 1: discard sample, default: 0 ;	only work for format 6
-split    <int>   split file ,1: \t or 2: \s+ , default \t; 
-help             output help information to screen
=head1 Exmple
perl find_identical.pl  xx.txt xx.txt -format 1
perl find_identical.pl  xx.fa  xx.txt -format 2 -site2 site
perl find_identical.pl  xx.fa  xx.fa  -format 3
perl find_identical.pl  xx.fq1 xx.fq2  xx.txt output.id -format 4 -site2 site
perl find_identical.pl  xx.txt xx.txt -format 5 -forward 1 
perl find_identical.pl  xx.txt xx.lst -format 6 -site1 site(sample start site) -site2 site
perl find_identical.pl  region.txt site.lst -format 7 -site2 site
perl find_identical.pl  xx.txt id1,id2,id3.. -format 8 
perl find_identical.pl  xx.txt -site1 5 -format 9 
perl find_identical.pl  xx.txt group.txt -site1 2 -format 10
perl find_identical.pl  xx.txt id1,id2,id3... -format 11
perl find_identical.pl  xx.txt xx.txt -format 12
region format
chr1	1000	2000
chr2	500	1000	xx	xx
....
group format
sam1	group1
sam2	group1
sam3	group2
...
=cut
my ($forward, $sysn, $split, $type, $format, $help, $site2, $site1, $head, $win); my (%hash1, %hash2);    my %rec1;   my %rec2;my $select;
GetOptions(
	"type:i"=>\$type,
	"format:i"=>\$format,
	"site1:i"=>\$site1,
	"site2:i"=>\$site2,
	"win:i"=>\$win,
	"head:i"=>\$head,
	"sysn:i"=>\$sysn,
	"select:i"=>\$select,
	"forward:i"=>\$forward,
	"split:i"=>\$split,
	"help"=>\$help
);
die `pod2text $0` if ($help || @ARGV < 1);
if($ARGV[0]=~/.gz$/){    open A, "gzip -dc $ARGV[0] |" or die "can not open $ARGV[0] \n$!";}
else{                    open A, $ARGV[0];}
if(defined $ARGV[1]){
	if($ARGV[1]=~/.gz$/){    open B, "gzip -dc $ARGV[1] |" or die "can not open $ARGV[1] \n$!";}
	else{                    open B, $ARGV[1];}
}
if(defined $ARGV[2]){
	if($ARGV[2]=~/.gz$/){  open C, "gzip -dc $ARGV[2] |" or die "can not open $ARGV[2] \n$!";}
	else{                  open C,$ARGV[2];}
	open O1,">$ARGV[3].1.fq";	open O2,">$ARGV[3].2.fq";
}
$type   ||=1; $site1  ||=1; $site2    ||=1; $format ||=1; $head	||=0;	$win ||=100000;	$site1=$site1-1;
$site2=$site2-1; $sysn	||=4;	$split	||=1;	$forward  ||=0;	$select	||=0;
my $str="0";
if($sysn == 1){	$str="_";}
if($sysn == 2){ $str="-";}
if($sysn == 3){ $str="NA";}
if($head == 1 && $format == 1 && ($type == 1 || $type ==4)){
	my $hh=<A>;	print "$hh";
}
if($head == 1 && $format == 9){
	<A>;
}
if($head == 2 && ($format == 1 || $format == 2) && ($type == 2 || $type ==5)){
	my $hh=<B>;     print "$hh";
}
if($head == 1 && $format == 5){
	my $hh1=<A>;    chomp $hh1; my $hh2=<B>;	chomp $hh2;
	if($forward == 0){ 	       print "$hh1\t$hh2\n";	}
	else{		  	       print "$hh2\t$hh1\n";	}
}
if($head == 3 && $format == 1 && ($type == 3 || $type == 6)){
	my $hh1=<A>;	chomp $hh1; my $hh2=<B>; 	print "$hh1\t$hh2";
}
my $len1=0;	my $len2=0;	my @poss;
if($format == 8){
	my $hh=<A>;	chomp $hh;	my @hh=split /\t/,$hh;
	if($split == 2){
		@hh=split /\s+/,$hh;
	}
	my @bb=split /,/,$ARGV[1];	print "$hh[0]";
	foreach my $kk(@bb){
		foreach my $k(1..$#hh){
			my $poss=$k+1;
			if($hh[$k] eq $kk){
				push @poss,$k;	print "\t$kk";	print STDERR"$kk\t$poss\n";
			}
		}
	}
	print "\n";
}
if($format == 6){
	while(<B>){    chomp;    my @b=split /\t/; if($split == 2){@b=split /\s+/;}  $hash2{$b[$site2]}=1; }
	my $hh1=<A>;	chomp $hh1;	my @hh1=split /\t/,$hh1;	if($split == 2){@hh1=split /\s+/,$hh1;}
	my $mmm1=join "\t",@hh1[0..($site1-1)];
	my %last;		print "$mmm1";
	foreach my $k($site1..$#hh1){
		if(exists $hash2{$hh1[$k]} && $select == 0){		$last{$k}=1;	print "\t$hh1[$k]";	}
		if(!exists $hash2{$hh1[$k]} && $select == 1){        	$last{$k}=1;    print "\t$hh1[$k]";	}
	}
	print "\n";
	while(<A>){
		chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;}
		my $mmm2=join "\t",@a[0..($site1-1)];		print "$mmm2";
		foreach my $k($site1..$#a){
			if(exists $last{$k}){           print "\t$a[$k]";	}
		}
		print "\n";
	}
}
if($format == 7){
	while(<B>){    chomp;    my @b=split /\t/; if($split == 2){@b=split /\s+/;}  $hash2{$b[0]}{$b[$site2]}=$_; }
	while(<A>){
		chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;}
		foreach my $k($a[1]..$a[2]){
			if(exists $hash2{$a[0]}{$k}){           print "$_\t$hash2{$a[0]}{$k}\n";       }
		}
	}
}
if($format == 8){
	while(<A>){
		chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;}
		print "$a[0]";
		foreach my $k(@poss){
			print "\t$a[$k]";
		}
		print "\n";
	}
}
if($format == 9){
	my %windos;
	print "Chr\tstart\tend\tnumber\n";
	while(<A>){
		chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;}
		my $int=int($a[$site1]/$win);	$windos{$a[0]}{$int}++;

	}
	foreach my $k1(sort keys %windos){
		foreach my $k2(sort {$a<=>$b} keys %{$windos{$k1}}){
			my $sss=$k2*$win;	my $eee=$sss+$win-1;
			print "$k1\t$sss\t$eee\t$windos{$k1}{$k2}\n";
		}
	}
}
if($format == 10){
	open A,$ARGV[1];        open B,$ARGV[0];
	my %map;        my %res;        my %site;       my %last;       my %samp;       my %ref;        my %file;
	while(<A>){
		chomp;my @a=split;      $map{$a[0]}=$a[1];      $res{$a[1]}=1;
	}
	close A;
	my $head=<B>;   chomp $head;    my @head=split /\s+/,$head;     my $mm=join "\t",@head[0..($site1-1)];
	foreach my $k(sort keys %res){
		my $handle; open $handle,">$k.group.txt";    $file{$k}=$handle;    print $handle "$mm";
	}
	foreach my $k($site1..$#head){
		$site{$k}=$head[$k];    next if(!exists $map{$head[$k]});
		my $out=$file{$map{$head[$k]}}; print $out "\t$head[$k]";
	}
	foreach my $k(sort keys %file){
		my $out=$file{$k};      print $out "\n";
	}
	while(<B>){
		chomp;my @a=split;      my $abc=join "\t",@a[0..($site1-1)];
		foreach my $k(sort keys %file){
			my $out=$file{$k};      print $out "$abc";
		}
		foreach my $k($site1..$#a){
			next if(!exists $map{$site{$k}});       my $out=$file{$map{$site{$k}}};         print $out "\t$a[$k]";
		}
		foreach my $k(sort keys %file){
			my $out=$file{$k};      print $out "\n";
		}
	}
	close B;
}
if($format == 11){
	open A,$ARGV[0];      my @order=split /,/,$ARGV[1];	my %map;	my $order=join "\t",@order;
	my $head=<A>;	chomp $head;	my @head=split /\t/,$head; if($split == 2){@head=split /\s+/,$head;}
	my $mm1=join "\t",@head[0..($site1-1)];
	foreach my $k($site1..$#head){
		$map{$head[$k]}=$k;
	}
	print "$mm1\t$order\n";
	while(<A>){
		chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;}
		my $mm2=join "\t",@a[0..($site1-1)];	print "$mm2";
		foreach my $k(@order){
			print "\t$a[$map{$k}]";
		}
		print "\n";
	}

}
if($format == 12){
        while(<A>){    chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;} $hash1{$a[0]}{$a[1]}=$a[2];}
        while(<B>){    chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;} 
			my ($min, $max)=($a[1], $a[2]);	my $record=0;
			foreach my $k(sort {$a<=>$b} keys %{$hash1{$a[0]}}){
			        last if($k > $a[2]);
		        	my @result=&compute_overlap($a[1],$a[2],$k,$hash1{$a[0]}{$k});
			        if($result[-1] > 0){
			            $record++;
			            if($k < $min){
		        	        $min=$k;
			            }
			            if($hash1{$a[0]}{$k} > $max){
                			$max=$hash1{$a[0]}{$k};
		        	    }
			        }
			}
			if($record > 0){
			        print "$a[0]\t$min\t$max\n";
		    	}
	}
}
if($format == 1){
	while(<A>){    chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;} $len1=@a;    $hash1{$a[$site1]}=$_;   $rec1{$.}=$a[$site1];}
	while(<B>){    chomp;    my @b=split /\t/; if($split == 2){@b=split /\s+/;} $len2=@b;    $hash2{$b[$site2]}=$_;   $rec2{$.}=$b[$site2];}
}
if($format == 5){
	while(<B>){    chomp;    my @b=split /\t/; if($split == 2){@b=split /\s+/;} $len2=@b;    $hash2{$b[$site2]}=$_;  }
	my @zeron=("$str") x $len2; my $zeron=join "\t",@zeron;
	while(<A>){
		chomp;my @a=split /\t/;	if($split == 2){@a=split /\s+/;}
		if($forward == 0){
			if(exists $hash2{$a[$site1]}){			print "$_\t$hash2{$a[$site1]}\n";	}
			else{						print "$_\t$zeron\n";			}
		}
		else{
			if(exists $hash2{$a[$site1]}){                  print "$hash2{$a[$site1]}\t$_\n";       }
			else{                                           print "$zeron\t$_\n";                   }
		}
	}
}
if($format == 2){
	$/=">";<A>;
	while(<A>){    chomp;    my @a=split /\n/,$_; my @b=split /\s+/,$a[0]; $hash1{$b[0]}=$_; $rec1{$.}=$b[0];}
	$/="\n";
	while(<B>){    chomp;    my @b=split /\t/,$_; if($split == 2){@b=split /\s+/;}    $hash2{$b[$site2]}=$_;   $rec2{$.}=$b[$site2];}
}
if($format == 3){
	$/=">";<A>;<B>;
	while(<A>){    chomp;    my @a=split /\n/,$_; my @b=split /\s+/,$a[0];    $hash1{$b[0]}=$_; $rec1{$.}=$b[0];}
	while(<B>){    chomp;    my @a=split /\n/,$_; my @b=split /\s+/,$a[0];    $hash2{$b[0]}=$_; $rec2{$.}=$b[0];}
	$/="\n";
}
if($format == 4){
	while(<C>){    chomp;    my @a=split /\t/; if($split == 2){@a=split /\s+/;} 	 my $id=$a[$site2]; $id="@".$id; $hash1{$a[$site2]}=1;	$hash1{$id}=1; }
	while(my $r1_1=<A>){
		chomp $r1_1;    my $r1_2=<A>; 	my $r1_3=<A>; my $r1_4=<A>;	my @a=split /\s+/,$r1_1;
		my $r2_1=<B>;  	my $r2_2=<B>;   my $r2_3=<B>; my $r2_4=<B>;  
		if($type == 1){
			if(exists $hash1{$a[0]}){
				print O1 "$r1_1\n$r1_2$r1_3$r1_4";		print O2 "$r2_1$r2_2$r2_3$r2_4";
			}
		}
		if($type == 4){
			if(!exists $hash1{$a[0]}){
				print O1 "$r1_1\n$r1_2$r1_3$r1_4";		print O2 "$r2_1$r2_2$r2_3$r2_4";
			}
		}
	}
	close C;close O1;close O2;
}
close A;close B;
if($type==4 || $type == 6){
	my %ppp1;
	foreach my $k(keys %hash2){
		if(exists $hash1{$k}){            $ppp1{$k}=1; }
	}
	foreach(sort {$a<=>$b} keys %rec1){
		next if(exists $ppp1{$rec1{$_}});
		if($format == 2 || $format == 3){            print ">$hash1{$rec1{$_}}";        }
		if($format == 1){ 
			if($type == 4){
				print "$hash1{$rec1{$_}}\n";
			}
			if($type == 6){
				my @zeron=($str) x $len2; my $zeron=join "\t",@zeron;  print "$hash1{$rec1{$_}}\t$zeron\n";       
			}
		}
	}
}
if($type==5 || $type == 6){
	my %ppp2;
	foreach my $k(keys %hash1){
		if(exists $hash2{$k}){           $ppp2{$k}=1;       }
	}
	foreach(sort keys %rec2){
		next if(exists $ppp2{$rec2{$_}});
		if($format == 3){            print ">$hash2{$rec2{$_}}";       }
		if($format == 1){
			if($type == 5){
				print "$hash2{$rec2{$_}}\n";        
			}
			if($type == 6){
				my @zeron=($str) x $len1; my $zeron=join "\t",@zeron;  print "$zeron\t$hash2{$rec2{$_}}\n";
			}
		}
		if($format == 2 && $type == 5){
			print "$hash2{$rec2{$_}}\n";
		}
	}
}
my @sort=sort {$a<=>$b} keys %rec1;
if($type == 2){    @sort=sort {$a<=>$b} keys %rec2;}
foreach my $k(@sort){
	if($type == 1 || $type == 3 || $type == 6){
		if(exists $hash2{$rec1{$k}}){
			if($type == 1){
				if($format == 2 || $format ==3){                    print ">$hash1{$rec1{$k}}";                 }
				else{                                               print "$hash1{$rec1{$k}}\n";                }
			}
			if($type == 3 || $type == 6){
				print "$hash1{$rec1{$k}}\t$hash2{$rec1{$k}}\n";
			}
		}
	}
	if($type == 2){
		if(exists $hash1{$rec2{$k}}){
			if($format == 3){                    print ">$hash2{$rec2{$k}}";                 }
			else{                                print "$hash2{$rec2{$k}}\n";                }
		}
	}
}


sub compute_overlap{
    my $x_start=shift;my $x_end=shift;my $y_start=shift;my $y_end=shift;
    my ($overlap_left, $overlap_right, $rate)=(0, 0, 0);my @result;
    if($y_start <= $x_end && $y_end >= $x_start){
        if($x_start >= $y_start){
            $overlap_left=$x_start;
        }
        else{
            $overlap_left=$y_start;
        }
        if($x_end <= $y_end){
            $overlap_right=$x_end;
        }
        else{
            $overlap_right=$y_end;
        }
    }
    my $overlap_len=$overlap_right-$overlap_left;
    if($x_end-$x_start >= $y_end-$y_start){
        $rate=$overlap_len/($y_end-$y_start+1);
    }
    else{
        $rate=$overlap_len/($x_end-$x_start+1);
    }
    push @result,$overlap_left;push @result,$overlap_right;push @result,$overlap_len;push @result,$rate;
    return @result;
}
