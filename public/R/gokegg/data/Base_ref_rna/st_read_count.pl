#!usr/bin/perl -w

use strict;

die "usage:perl $0 <all.read> <out: align_sum.txt> <out:prefix> <clean.data.txt>\n" unless @ARGV == 4;

open A,$ARGV[0];	open B,"$ARGV[0].summary";	open C,$ARGV[3];
open O1,">$ARGV[1]";	open O2,">$ARGV[2].read.txt";	open O3,">$ARGV[2].read.txt.t";	open O4,">$ARGV[2].fpkm.txt";
open O5,">$ARGV[2].tpm.txt";
<A>;<C>;
my $head=<A>;	chomp $head;my @head=split /\t/,$head;
my %id;	my %map;	
while(<C>){
	chomp;my @a=split;$map{$a[0]}[0]=$a[1];
}
close C;
my @sam;
foreach my $k(6..$#head){
	my @p1=split /\//,$head[$k];my @p2=split /\.sort/,$p1[-1];push @sam,$p2[0]; my $site=$k-5;	$id{$site}=$p2[0];
}
my $sam=join "\t",@sam;
print O2 "$head[0]\t$sam\n";	print O3 "$sam\n";	print O4 "$head[0]\t$sam\n";	print O5 "$head[0]\t$sam\n";
while(<B>){
	chomp;my @a=split;
	if(/Unassigned_Unmapped/){
		foreach my $k(1..$#a){
			$map{$id{$k}}[1]=$map{$id{$k}}[0]-2*$a[$k];
		}
	}
	if($a[0] eq "Assigned"){
		foreach my $k(1..$#a){
			$map{$id{$k}}[2]=2*$a[$k];
		}
	}
}
my %acc;
while(<A>){
	chomp;my @a=split /\t/;		my $pp=join "\t",@a[6..$#a];
	print O2 "$a[0]\t$pp\n";	print O3 "$a[0]\t$pp\n";	print O4 "$a[0]"; my $len=$a[5]/1000;
	foreach my $k(6..$#a){
		$acc{$k}+=$a[$k]/$len;
		my $site=$k-5;	my $rpkm=1000000*$a[$k]/($map{$id{$site}}[2]*$len);
		printf O4 "\t%.3f",$rpkm;
	}
	print O4 "\n";
}
close A;	open A,$ARGV[0];	<A>;<A>;
while(<A>){
        chomp;my @a=split /\t/;         print O5 "$a[0]";
        my $len=$a[5]/1000;
        foreach my $k(6..$#a){
                my $tpm=1000000*$a[$k]/($acc{$k}*$len);
                printf O5 "\t%.3f",$tpm;
        }
        print O5 "\n";
}
print O1 "Sample\tClean reads\tMapped reads\tMapped rate(%)\n";
foreach my $k(sort {$a<=>$b} keys %id){
	printf O1 "%s\t%d\t%d\t%2f\n",$id{$k},$map{$id{$k}}[0],$map{$id{$k}}[1],100*$map{$id{$k}}[1]/$map{$id{$k}}[0];
}
